package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

sealed class PrinterStatus {
    object Disconnected : PrinterStatus()
    data class Connecting(val deviceName: String) : PrinterStatus()
    data class Connected(val deviceName: String, val deviceAddress: String, val isSimulated: Boolean = false) : PrinterStatus()
    data class Printing(val message: String) : PrinterStatus()
    data class Error(val message: String) : PrinterStatus()
}

data class DiscoveredPrinter(
    val name: String,
    val address: String,
    val isPaired: Boolean = true
)

class BluetoothPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "POSPrinterManager"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS_NAME = "printer_prefs"
        private const val KEY_LAST_DEVICE_ADDR = "last_printer_addr"
        private const val KEY_LAST_DEVICE_NAME = "last_printer_name"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = try {
        bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    } catch (_: Exception) {
        null
    }

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _printerStatus = MutableStateFlow<PrinterStatus>(PrinterStatus.Disconnected)
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()

    private val _lastPrintedReceipt = MutableStateFlow<String?>(null)
    val lastPrintedReceipt: StateFlow<String?> = _lastPrintedReceipt.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredPrinter>> = _discoveredDevices.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var isReceiverRegistered = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                            disconnectInternal()
                            _printerStatus.value = PrinterStatus.Disconnected
                        }
                    }
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let {
                            val name = try {
                                it.name?.takeIf { n -> n.isNotBlank() } ?: "Perangkat Bluetooth"
                            } catch (_: SecurityException) {
                                "Perangkat Bluetooth"
                            } catch (_: Exception) {
                                "Perangkat Bluetooth"
                            }
                            val addr = it.address ?: return@let
                            val isBonded = try {
                                it.bondState == BluetoothDevice.BOND_BONDED
                            } catch (_: SecurityException) {
                                false
                            } catch (_: Exception) {
                                false
                            }
                            val printerItem = DiscoveredPrinter(name = name, address = addr, isPaired = isBonded)

                            val currentList = _discoveredDevices.value.toMutableList()
                            if (currentList.none { d -> d.address.equals(addr, ignoreCase = true) }) {
                                currentList.add(printerItem)
                                _discoveredDevices.value = currentList
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        _isScanning.value = true
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        _isScanning.value = false
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val currentStatus = _printerStatus.value
                        if (currentStatus is PrinterStatus.Connected && !currentStatus.isSimulated) {
                            if (device?.address == currentStatus.deviceAddress) {
                                disconnectInternal()
                                _printerStatus.value = PrinterStatus.Disconnected
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in bluetoothReceiver.onReceive", e)
            }
        }
    }

    init {
        checkBluetoothEnabled()
        registerReceiverSafely()
    }

    private fun registerReceiverSafely() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Must specify RECEIVER_EXPORTED for system broadcast actions
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Safe receiver registration fallback", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), Context.RECEIVER_NOT_EXPORTED)
                    isReceiverRegistered = true
                }
            } catch (_: Exception) {}
        }
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun checkBluetoothEnabled(): Boolean {
        val enabled = try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
        _isBluetoothEnabled.value = enabled
        return enabled
    }

    fun getLastSavedDevice(): DiscoveredPrinter? {
        val addr = sharedPrefs.getString(KEY_LAST_DEVICE_ADDR, null) ?: return null
        val name = sharedPrefs.getString(KEY_LAST_DEVICE_NAME, "Printer") ?: "Printer"
        return DiscoveredPrinter(name = name, address = addr, isPaired = true)
    }

    private fun saveLastDevice(name: String, address: String) {
        sharedPrefs.edit()
            .putString(KEY_LAST_DEVICE_ADDR, address)
            .putString(KEY_LAST_DEVICE_NAME, name)
            .apply()
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<DiscoveredPrinter> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!hasBluetoothPermission()) return emptyList()

        return try {
            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                val name = try {
                    device.name?.takeIf { it.isNotBlank() } ?: "Thermal Printer"
                } catch (_: SecurityException) {
                    "Thermal Printer"
                } catch (_: Exception) {
                    "Thermal Printer"
                }
                DiscoveredPrinter(
                    name = name,
                    address = device.address,
                    isPaired = true
                )
            }.sortedBy { it.name }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!checkBluetoothEnabled()) return false
        if (!hasScanPermission()) return false

        return try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on startDiscovery", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (hasScanPermission() && bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            _isScanning.value = false
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        deviceAddress: String,
        deviceName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (deviceAddress == "SIMULATION") {
            _printerStatus.value = PrinterStatus.Connected("Printer Virtual (Simulasi)", "00:00:00:00:00:00", isSimulated = true)
            onResult(true, "Terhubung ke Printer Virtual")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !checkBluetoothEnabled()) {
            _printerStatus.value = PrinterStatus.Error("Bluetooth tidak aktif. Silakan nyalakan Bluetooth.")
            onResult(false, "Bluetooth belum aktif")
            return
        }

        if (!hasBluetoothPermission()) {
            _printerStatus.value = PrinterStatus.Error("Izin Bluetooth belum diberikan di perangkat.")
            onResult(false, "Izin Bluetooth diperlukan")
            return
        }

        _printerStatus.value = PrinterStatus.Connecting(deviceName)

        CoroutineScope(Dispatchers.IO).launch {
            stopDiscovery()
            disconnectInternal()

            val connected = withTimeoutOrNull(10000L) {
                tryConnectWithFallback(adapter, deviceAddress)
            }

            if (connected != null && connected.isConnected) {
                activeSocket = connected
                try {
                    outputStream = connected.outputStream
                    saveLastDevice(deviceName, deviceAddress)
                    _printerStatus.value = PrinterStatus.Connected(deviceName, deviceAddress, isSimulated = false)

                    // Wake-up clear buffer
                    try {
                        outputStream?.write(EscPosHelper.CMD_INIT)
                        outputStream?.flush()
                    } catch (_: Exception) {}

                    withContext(Dispatchers.Main) {
                        onResult(true, "Berhasil terhubung ke $deviceName")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error acquiring output stream", e)
                    disconnectInternal()
                    _printerStatus.value = PrinterStatus.Error("Gagal membuka saluran data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        onResult(false, "Gagal membuka saluran printer: ${e.message}")
                    }
                }
            } else {
                disconnectInternal()
                val msg = "Gagal terhubung ke $deviceName. Pastikan printer menyala & di-pair dengan PIN 0000 atau 1234."
                _printerStatus.value = PrinterStatus.Error(msg)
                withContext(Dispatchers.Main) {
                    onResult(false, msg)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryConnectWithFallback(adapter: BluetoothAdapter, address: String): BluetoothSocket? {
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid MAC address: $address", e)
            return null
        }

        var socket: BluetoothSocket? = null

        // 1. Standard SPP
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            if (socket.isConnected) return socket
        } catch (_: Exception) {
            try { socket?.close() } catch (_: Exception) {}
        }

        // 2. Insecure SPP
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            if (socket.isConnected) return socket
        } catch (_: Exception) {
            try { socket?.close() } catch (_: Exception) {}
        }

        // 3. Reflection Channel 1
        try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            socket = method.invoke(device, 1) as BluetoothSocket
            socket.connect()
            if (socket.isConnected) return socket
        } catch (_: Exception) {
            try { socket?.close() } catch (_: Exception) {}
        }

        // 4. Insecure Reflection Channel 1
        try {
            val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
            socket = method.invoke(device, 1) as BluetoothSocket
            socket.connect()
            if (socket.isConnected) return socket
        } catch (_: Exception) {
            try { socket?.close() } catch (_: Exception) {}
        }

        return null
    }

    fun disconnect() {
        disconnectInternal()
        _printerStatus.value = PrinterStatus.Disconnected
    }

    private fun disconnectInternal() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        outputStream = null
        activeSocket = null
    }

    fun sendBytes(
        data: ByteArray,
        previewText: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val current = _printerStatus.value

        if (current is PrinterStatus.Connected && current.isSimulated) {
            _printerStatus.value = PrinterStatus.Printing("Mencetak ke Printer Virtual...")
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(400)
                if (previewText != null) {
                    _lastPrintedReceipt.value = previewText
                }
                _printerStatus.value = current
                withContext(Dispatchers.Main) {
                    onComplete(true, "Struk dicetak (Mode Simulasi)!")
                }
            }
            return
        }

        val out = outputStream
        if (out == null || activeSocket?.isConnected != true) {
            onComplete(false, "Printer belum terhubung. Hubungkan printer di tab 'Koneksi BT'.")
            return
        }

        _printerStatus.value = PrinterStatus.Printing("Mengirim data...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chunkSize = 512
                var offset = 0
                while (offset < data.size) {
                    val len = (data.size - offset).coerceAtMost(chunkSize)
                    out.write(data, offset, len)
                    out.flush()
                    offset += len
                    if (offset < data.size) {
                        kotlinx.coroutines.delay(20)
                    }
                }

                if (previewText != null) {
                    _lastPrintedReceipt.value = previewText
                }
                _printerStatus.value = current
                withContext(Dispatchers.Main) {
                    onComplete(true, "Struk berhasil dicetak!")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error writing stream", e)
                disconnectInternal()
                _printerStatus.value = PrinterStatus.Error("Koneksi terputus: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false, "Gagal mencetak: ${e.message}")
                }
            }
        }
    }

    fun printCustomText(text: String, onComplete: (Boolean, String?) -> Unit) {
        val stream = java.io.ByteArrayOutputStream()
        stream.write(EscPosHelper.CMD_INIT)
        stream.write(EscPosHelper.CMD_ALIGN_CENTER)
        stream.write(EscPosHelper.CMD_BOLD_ON)
        stream.write("TES CETAK PRINTER\n".toByteArray(Charsets.ISO_8859_1))
        stream.write(EscPosHelper.CMD_BOLD_OFF)
        stream.write(EscPosHelper.CMD_ALIGN_LEFT)
        stream.write("$text\n\n\n\n".toByteArray(Charsets.ISO_8859_1))
        stream.write(EscPosHelper.CMD_FEED_AND_CUT)

        sendBytes(stream.toByteArray(), previewText = text, onComplete = onComplete)
    }

    fun connectSimulatedPrinter() {
        _printerStatus.value = PrinterStatus.Connected("Printer Virtual (Simulasi)", "00:00:00:00:00:00", isSimulated = true)
    }

    fun destroy() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {}
        }
        disconnectInternal()
    }
}
