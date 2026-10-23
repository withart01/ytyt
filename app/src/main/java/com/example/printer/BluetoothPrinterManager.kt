package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        // Standard Serial Port Profile (SPP) UUID used by virtually all thermal ESC/POS printers
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS_NAME = "printer_prefs"
        private const val KEY_LAST_DEVICE_ADDR = "last_printer_addr"
        private const val KEY_LAST_DEVICE_NAME = "last_printer_name"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _printerStatus = MutableStateFlow<PrinterStatus>(PrinterStatus.Disconnected)
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()

    private val _lastPrintedReceipt = MutableStateFlow<String?>(null)
    val lastPrintedReceipt: StateFlow<String?> = _lastPrintedReceipt.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

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
        return try {
            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                DiscoveredPrinter(
                    name = device.name ?: "Perangkat Tanpa Nama",
                    address = device.address,
                    isPaired = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        deviceAddress: String,
        deviceName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (deviceAddress == "SIMULATION") {
            // Virtual/Simulated printer for testing in emulator
            _printerStatus.value = PrinterStatus.Connected("Printer Virtual (Simulasi)", "00:00:00:00:00:00", isSimulated = true)
            onResult(true, "Terhubung ke Printer Virtual")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _printerStatus.value = PrinterStatus.Error("Bluetooth tidak aktif")
            onResult(false, "Bluetooth belum diaktifkan")
            return
        }

        _printerStatus.value = PrinterStatus.Connecting(deviceName)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                disconnectInternal()

                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                // Standard SPP connection
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                adapter.cancelDiscovery()
                socket.connect()

                activeSocket = socket
                outputStream = socket.outputStream

                saveLastDevice(deviceName, deviceAddress)
                _printerStatus.value = PrinterStatus.Connected(deviceName, deviceAddress, isSimulated = false)
                withContext(Dispatchers.Main) {
                    onResult(true, "Berhasil terhubung ke $deviceName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to printer", e)
                disconnectInternal()
                _printerStatus.value = PrinterStatus.Error("Gagal terhubung: ${e.localizedMessage ?: "Koneksi ditolak"}")
                withContext(Dispatchers.Main) {
                    onResult(false, "Gagal koneksi ke printer: ${e.message}")
                }
            }
        }
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

    /**
     * Sends binary ESC/POS data to the connected printer, or captures it in simulation mode.
     */
    fun sendBytes(
        data: ByteArray,
        previewText: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val current = _printerStatus.value

        if (current is PrinterStatus.Connected && current.isSimulated) {
            // Simulated printer success
            _printerStatus.value = PrinterStatus.Printing("Mencetak ke Printer Virtual...")
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(600)
                if (previewText != null) {
                    _lastPrintedReceipt.value = previewText
                }
                _printerStatus.value = current
                withContext(Dispatchers.Main) {
                    onResultMain(true, "Struk berhasil dicetak ke Printer Virtual!", onComplete)
                }
            }
            return
        }

        val out = outputStream
        if (out == null || activeSocket?.isConnected != true) {
            onComplete(false, "Printer belum terhubung. Hubungkan printer di menu Printer.")
            return
        }

        _printerStatus.value = PrinterStatus.Printing("Mengirim data ke printer...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                out.write(data)
                out.flush()
                if (previewText != null) {
                    _lastPrintedReceipt.value = previewText
                }
                _printerStatus.value = current
                withContext(Dispatchers.Main) {
                    onResultMain(true, "Struk berhasil dicetak!", onComplete)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error writing to printer stream", e)
                disconnectInternal()
                _printerStatus.value = PrinterStatus.Error("Koneksi terputus saat mencetak: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResultMain(false, "Gagal mencetak: ${e.message}", onComplete)
                }
            }
        }
    }

    private fun onResultMain(success: Boolean, msg: String, callback: (Boolean, String?) -> Unit) {
        callback(success, msg)
    }

    fun connectSimulatedPrinter() {
        _printerStatus.value = PrinterStatus.Connected("Printer Virtual (Simulasi)", "00:00:00:00:00:00", isSimulated = true)
    }
}
