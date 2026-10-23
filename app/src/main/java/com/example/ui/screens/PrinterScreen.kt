package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreSettings
import com.example.printer.BluetoothPrinterManager
import com.example.printer.DiscoveredPrinter
import com.example.printer.PrinterStatus

@Composable
fun PrinterScreen(
    printerManager: BluetoothPrinterManager,
    printerStatus: PrinterStatus,
    storeSettings: StoreSettings,
    onUpdateStoreSettings: (StoreSettings) -> Unit,
    onTestPrint: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf<List<DiscoveredPrinter>>(emptyList()) }
    var isSimulatedMode by remember { mutableStateOf(printerStatus is PrinterStatus.Connected && printerStatus.isSimulated) }

    // Store settings fields
    var storeName by remember(storeSettings) { mutableStateOf(storeSettings.storeName) }
    var address by remember(storeSettings) { mutableStateOf(storeSettings.address) }
    var phone by remember(storeSettings) { mutableStateOf(storeSettings.phone) }
    var footerMsg by remember(storeSettings) { mutableStateOf(storeSettings.footerMessage) }
    var paperSize by remember(storeSettings) { mutableStateOf(storeSettings.paperSize) }

    // Permission launcher for Android 6 Marshmallow to Android 14+
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            pairedDevices = printerManager.getPairedPrinters()
            onShowMessage("Izin Bluetooth diberikan. Menemukan ${pairedDevices.size} perangkat.")
        } else {
            onShowMessage("Izin Bluetooth ditolak. Anda tetap dapat menggunakan Printer Virtual.")
        }
    }

    fun loadDevices() {
        permissionLauncher.launch(permissionsToRequest)
    }

    LaunchedEffect(Unit) {
        pairedDevices = printerManager.getPairedPrinters()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("printer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Printer Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (printerStatus) {
                    is PrinterStatus.Connected -> Color(0xFFF0FDF4)
                    is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFFFEF9C3)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (printerStatus) {
                                is PrinterStatus.Connected -> Color(0xFF16A34A)
                                is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFFCA8A04)
                                else -> Color(0xFF64748B)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (printerStatus is PrinterStatus.Connected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (printerStatus) {
                                    is PrinterStatus.Connected -> "Printer Terhubung"
                                    is PrinterStatus.Connecting -> "Menghubungkan..."
                                    is PrinterStatus.Printing -> "Sedang Mencetak..."
                                    is PrinterStatus.Error -> "Koneksi Bermasalah"
                                    else -> "Printer Belum Terhubung"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (printerStatus) {
                                    is PrinterStatus.Connected -> "${printerStatus.deviceName} (${printerStatus.deviceAddress})"
                                    is PrinterStatus.Connecting -> printerStatus.deviceName
                                    is PrinterStatus.Printing -> printerStatus.message
                                    is PrinterStatus.Error -> printerStatus.message
                                    else -> "Pilih perangkat Bluetooth di bawah"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (printerStatus is PrinterStatus.Connected) {
                        OutlinedButton(
                            onClick = {
                                printerManager.disconnect()
                                isSimulatedMode = false
                                onShowMessage("Printer terputus")
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Putus", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (printerStatus is PrinterStatus.Connected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onTestPrint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("test_print_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uji Coba Cetak (Test Print)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Virtual Printer Mode Toggle (Crucial for emulators / tests)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mode Printer Virtual (Simulasi)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gunakan ini untuk mencoba cetak struk tanpa perangkat fisik Bluetooth",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isSimulatedMode,
                    onCheckedChange = { enable ->
                        isSimulatedMode = enable
                        if (enable) {
                            printerManager.connectSimulatedPrinter()
                            onShowMessage("Mode Printer Virtual aktif!")
                        } else {
                            printerManager.disconnect()
                        }
                    },
                    modifier = Modifier.testTag("simulation_switch")
                )
            }
        }

        // Paired Devices List Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Perangkat Bluetooth Paired",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { loadDevices() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("refresh_devices_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Muat Ulang", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (pairedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada printer Bluetooth yang di-pair.\nPastikan printer dinyalakan & di-pair di Pengaturan HP.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pairedDevices.forEach { device ->
                            val isThisConnected = printerStatus is PrinterStatus.Connected && printerStatus.deviceAddress == device.address
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        printerManager.connectToDevice(device.address, device.name) { success, msg ->
                                            if (msg != null) onShowMessage(msg)
                                        }
                                    }
                                    .testTag("device_item_${device.address}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isThisConnected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = device.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = device.address,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isThisConnected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF16A34A)
                                        ) {
                                            Text(
                                                text = "Terhubung",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                printerManager.connectToDevice(device.address, device.name) { success, msg ->
                                                    if (msg != null) onShowMessage(msg)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Hubungkan", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Store Profile & Receipt Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengaturan Header & Struk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                // Paper width
                Text("Ukuran Kertas Thermal:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(58 to "58 mm (Standar)", 80 to "80 mm (Besar)").forEach { (size, label) ->
                        val isSelected = paperSize == size
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paperSize = size },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nama Toko / Usaha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Toko") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. WhatsApp / Telepon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = footerMsg,
                    onValueChange = { footerMsg = it },
                    label = { Text("Pesan Penutup Struk") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        val updated = storeSettings.copy(
                            storeName = storeName.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            footerMessage = footerMsg.trim(),
                            paperSize = paperSize
                        )
                        onUpdateStoreSettings(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_store_settings_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Pengaturan Struk", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
