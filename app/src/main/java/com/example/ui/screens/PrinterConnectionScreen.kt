package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.BluetoothPrinterManager
import com.example.printer.DiscoveredPrinter
import com.example.printer.PrinterStatus

@Composable
fun PrinterConnectionScreen(
    printerManager: BluetoothPrinterManager,
    printerStatus: PrinterStatus,
    onTestPrint: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBtEnabled by printerManager.isBluetoothEnabled.collectAsState()
    val isScanning by printerManager.isScanning.collectAsState()
    val discoveredDevices by printerManager.discoveredDevices.collectAsState()

    var pairedDevices by remember { mutableStateOf<List<DiscoveredPrinter>>(emptyList()) }
    var showCustomPrintDialog by remember { mutableStateOf(false) }
    var customPrintText by remember { mutableStateOf("") }

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
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            pairedDevices = printerManager.getPairedPrinters()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        printerManager.checkBluetoothEnabled()
        pairedDevices = printerManager.getPairedPrinters()
    }

    LaunchedEffect(Unit) {
        printerManager.checkBluetoothEnabled()
        pairedDevices = printerManager.getPairedPrinters()
    }

    // Merge paired and discovered devices without duplicate MAC addresses
    val allPrinters = remember(pairedDevices, discoveredDevices) {
        val list = pairedDevices.toMutableList()
        discoveredDevices.forEach { dev ->
            if (list.none { it.address.equals(dev.address, ignoreCase = true) }) {
                list.add(dev)
            }
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("printer_connection_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Item 1: Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = when (printerStatus) {
                            is PrinterStatus.Connected -> Color(0xFF10B981)
                            is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFFF59E0B)
                            else -> Color(0xFF334155)
                        },
                        shape = RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (printerStatus) {
                                        is PrinterStatus.Connected -> Color(0xFF065F46)
                                        is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFF78350F)
                                        else -> Color(0xFF334155)
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (printerStatus is PrinterStatus.Connected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = when (printerStatus) {
                                                is PrinterStatus.Connected -> Color(0xFF34D399)
                                                is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFFFBBF24)
                                                else -> Color(0xFF94A3B8)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(
                                                color = when (printerStatus) {
                                                    is PrinterStatus.Connected -> Color(0xFF34D399)
                                                    is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFFFBBF24)
                                                    else -> Color(0xFF64748B)
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (printerStatus) {
                                            is PrinterStatus.Connected -> "TERHUBUNG"
                                            is PrinterStatus.Connecting -> "MENGHUBUNGKAN..."
                                            is PrinterStatus.Printing -> "MENCETAK..."
                                            is PrinterStatus.Error -> "GAGAL TERHUBUNG"
                                            else -> if (!isBtEnabled) "BLUETOOTH NONAKTIF" else "BELUM TERHUBUNG"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (printerStatus) {
                                            is PrinterStatus.Connected -> Color(0xFF34D399)
                                            is PrinterStatus.Connecting, is PrinterStatus.Printing -> Color(0xFFFBBF24)
                                            else -> if (!isBtEnabled) Color(0xFFF87171) else Color(0xFF94A3B8)
                                        }
                                    )
                                }

                                Text(
                                    text = when (printerStatus) {
                                        is PrinterStatus.Connected -> printerStatus.deviceName
                                        is PrinterStatus.Connecting -> printerStatus.deviceName
                                        is PrinterStatus.Printing -> "Mengirim data struk..."
                                        is PrinterStatus.Error -> "Koneksi terputus/ditolak"
                                        else -> if (!isBtEnabled) "Nyalakan Bluetooth untuk koneksi" else "Silakan pilih printer di bawah"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )

                                if (printerStatus is PrinterStatus.Connected) {
                                    Text(
                                        text = printerStatus.deviceAddress,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        if (printerStatus is PrinterStatus.Connected) {
                            OutlinedButton(
                                onClick = {
                                    printerManager.disconnect()
                                    onShowMessage("Printer terputus")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                                modifier = Modifier.testTag("disconnect_printer_btn")
                            ) {
                                Text("Putus", fontSize = 11.sp, color = Color(0xFFEF4444))
                            }
                        } else if (!isBtEnabled) {
                            Button(
                                onClick = {
                                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    enableBtLauncher.launch(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Nyalakan", fontSize = 11.sp)
                            }
                        }
                    }

                    if (printerStatus is PrinterStatus.Connected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTestPrint,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(40.dp)
                                    .testTag("test_print_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uji Coba Cetak", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showCustomPrintDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("custom_print_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cetak Teks", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        }

        // Item 2: Find & Device Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pilih Printer Bluetooth",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${allPrinters.size} perangkat ditemukan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isScanning) {
                        OutlinedButton(
                            onClick = { printerManager.stopDiscovery() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Batal", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                permissionLauncher.launch(permissionsToRequest)
                                val ok = printerManager.startDiscovery()
                                if (ok) {
                                    onShowMessage("Mencari printer thermal sekitar...")
                                } else {
                                    onShowMessage("Pastikan Bluetooth aktif.")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("start_scan_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cari Printer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (isScanning) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sedang memindai printer sekitar...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (allPrinters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada printer Bluetooth yang ditemukan.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nyalakan printer lalu klik 'Cari Printer' di atas",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            items(allPrinters, key = { it.address }) { device ->
                val isConnected = printerStatus is PrinterStatus.Connected && printerStatus.deviceAddress == device.address
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            printerManager.connectToDevice(device.address, device.name) { success, msg ->
                                if (msg != null) onShowMessage(msg)
                            }
                        }
                        .testTag("printer_device_${device.address}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isConnected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surface,
                    border = if (isConnected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = device.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isConnected) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF16A34A)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terhubung", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    printerManager.connectToDevice(device.address, device.name) { success, msg ->
                                        if (msg != null) onShowMessage(msg)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Hubungkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Helper Button: Open Phone Bluetooth Settings
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_phone_bt_settings"),
                colors = ButtonDefaults.outlinedButtonColors(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pair Printer di Pengaturan Bluetooth HP (PIN: 0000 / 1234)", fontSize = 11.sp)
            }
        }
    }

    // Custom Text Print Dialog
    if (showCustomPrintDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCustomPrintDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.95f),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cetak Teks Kustom Langsung",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ketik teks untuk memeriksa respon cetak pada printer thermal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customPrintText,
                        onValueChange = { customPrintText = it },
                        label = { Text("Teks untuk dicetak") },
                        placeholder = { Text("Contoh: Selamat Pagi! Printer berfungsi sempurna.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showCustomPrintDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customPrintText.isNotBlank()) {
                                    printerManager.printCustomText(customPrintText) { success, msg ->
                                        if (msg != null) onShowMessage(msg)
                                    }
                                    showCustomPrintDialog = false
                                }
                            },
                            enabled = customPrintText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Kirim ke Printer")
                        }
                    }
                }
            }
        }
    }
}
