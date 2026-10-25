package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.printer.PrinterStatus
import com.example.ui.components.ThermalPaperView

@Composable
fun ReceiptFormatScreen(
    storeSettings: StoreSettings,
    printerStatus: PrinterStatus,
    onUpdateStoreSettings: (StoreSettings) -> Unit,
    onTestPrint: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var storeName by remember(storeSettings) { mutableStateOf(storeSettings.storeName) }
    var address by remember(storeSettings) { mutableStateOf(storeSettings.address) }
    var phone by remember(storeSettings) { mutableStateOf(storeSettings.phone) }
    var footerMsg by remember(storeSettings) { mutableStateOf(storeSettings.footerMessage) }
    var cashierName by remember(storeSettings) { mutableStateOf(storeSettings.cashierName) }
    var paperSize by remember(storeSettings) { mutableIntStateOf(storeSettings.paperSize) }

    // State untuk Ukuran Font & Baris Template
    var headerFontSize by remember(storeSettings) { mutableStateOf(storeSettings.headerFontSize) }
    var showAddress by remember(storeSettings) { mutableStateOf(storeSettings.showAddress) }
    var showPhone by remember(storeSettings) { mutableStateOf(storeSettings.showPhone) }
    var showCashier by remember(storeSettings) { mutableStateOf(storeSettings.showCashier) }
    var showDateTime by remember(storeSettings) { mutableStateOf(storeSettings.showDateTime) }
    var showFooter by remember(storeSettings) { mutableStateOf(storeSettings.showFooter) }

    // Live preview settings reflecting current inputs and toggles
    val previewSettings = remember(
        storeName, address, phone, footerMsg, cashierName, paperSize,
        headerFontSize, showAddress, showPhone, showCashier, showDateTime, showFooter
    ) {
        StoreSettings(
            storeName = storeName.ifBlank { "PT SARASA FOOD" },
            address = address,
            phone = phone,
            footerMessage = footerMsg,
            cashierName = cashierName.ifBlank { "Kasir 1" },
            paperSize = paperSize,
            headerFontSize = headerFontSize,
            showAddress = showAddress,
            showPhone = showPhone,
            showCashier = showCashier,
            showDateTime = showDateTime,
            showFooter = showFooter
        )
    }

    val sampleTx = remember(previewSettings) {
        TransactionEntity(
            invoiceNumber = "INV-2026-001",
            subtotal = 38000,
            grandTotal = 38000,
            paymentMethod = "Tunai",
            cashReceived = 50000,
            changeAmount = 12000,
            cashierName = previewSettings.cashierName,
            customerName = "Meja 05",
            itemsSummary = "Kopi Susu (1x), Nasi Goreng (1x)",
            itemsJson = ""
        )
    }

    val sampleItems = remember {
        listOf(
            CartItem(name = "Kopi Susu Gula Aren", price = 18000, quantity = 1),
            CartItem(name = "Nasi Goreng Spesial", price = 20000, quantity = 1)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("receipt_format_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Format & Template Struk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Atur ukuran huruf & kelola baris template struk",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onTestPrint,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("test_print_format_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tes Cetak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 1: Paper Size Selection (58mm vs 80mm)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ukuran Kertas Thermal Printer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        58 to "58 mm (32 Kolom)\nPrinter Mini Portable",
                        80 to "80 mm (48 Kolom)\nPrinter POS Desktop"
                    ).forEach { (size, label) ->
                        val isSelected = paperSize == size
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { paperSize = size }
                                .testTag("paper_size_$size"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Store Header & Typography Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Baris 1: Nama Toko (Header Utama)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Wajib",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nama Toko / Usaha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Tombol Ukuran Font Nama Toko (Besar / Sedang / Normal)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Opsi Ukuran Huruf (Font Size) Nama Toko:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("BESAR", "Besar (2x)", "ESC 2x2"),
                            Triple("SEDANG", "Sedang", "ESC 1x2"),
                            Triple("NORMAL", "Normal", "ESC 1x1")
                        ).forEach { (sizeKey, title, sub) ->
                            val isSelected = headerFontSize == sizeKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { headerFontSize = sizeKey },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = sub,
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Template Line Blocks (Alamat, Telepon, Kasir, Tanggal, Footer)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Kelola Baris Template Struk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Anda dapat menghapus/menyembunyikan baris template jika tidak ingin dicetak, atau mengaktifkannya kembali kapan saja.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Template Baris Alamat Toko
                TemplateRowItem(
                    title = "Baris Alamat Toko",
                    icon = Icons.Default.LocationOn,
                    isActive = showAddress,
                    onToggleActive = { showAddress = it },
                    content = {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Toko") },
                            placeholder = { Text("Contoh: Jl. Sudirman No. 128") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )

                // 2. Template Baris Nomor Telepon
                TemplateRowItem(
                    title = "Baris No. WhatsApp / Telepon",
                    icon = Icons.Default.Phone,
                    isActive = showPhone,
                    onToggleActive = { showPhone = it },
                    content = {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("No. WhatsApp / Telepon Toko") },
                            placeholder = { Text("Contoh: 0812-3456-7890") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )

                // 3. Template Baris Tanggal & Waktu
                TemplateRowItem(
                    title = "Baris Tanggal & Jam Transaksi",
                    icon = Icons.Default.DateRange,
                    isActive = showDateTime,
                    onToggleActive = { showDateTime = it },
                    content = {
                        Text(
                            text = "Format waktu otomatis tercetak sesuai jam saat transaksi kasir diselesaikan (Contoh: 24/10/2026 14:30).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // 4. Template Baris Identitas Kasir
                TemplateRowItem(
                    title = "Baris Nama Kasir",
                    icon = Icons.Default.Person,
                    isActive = showCashier,
                    onToggleActive = { showCashier = it },
                    content = {
                        OutlinedTextField(
                            value = cashierName,
                            onValueChange = { cashierName = it },
                            label = { Text("Nama Kasir Default") },
                            placeholder = { Text("Kasir 1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )

                // 5. Value Product Block (PROTECTED / WAJIB)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4), // soft green
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rincian & Nilai Produk (Value Product)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Terkunci (Wajib)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rincian produk (daftar belanja, qty, harga satuan, diskon, dan total pembayaran) tidak dapat dihapus agar struk sah sebagai bukti transaksi jual-beli.",
                            fontSize = 11.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }

                // 6. Template Baris Pesan Penutup (Footer)
                TemplateRowItem(
                    title = "Baris Catatan Footer Struk",
                    icon = Icons.Default.Receipt,
                    isActive = showFooter,
                    onToggleActive = { showFooter = it },
                    content = {
                        OutlinedTextField(
                            value = footerMsg,
                            onValueChange = { footerMsg = it },
                            label = { Text("Pesan Penutup Struk (Footer)") },
                            placeholder = { Text("Contoh: Terima kasih atas kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar.") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )

                // Tombol Simpan
                Button(
                    onClick = {
                        val updated = storeSettings.copy(
                            storeName = storeName.trim().ifBlank { "PT SARASA FOOD" },
                            address = address.trim(),
                            phone = phone.trim(),
                            footerMessage = footerMsg.trim(),
                            cashierName = cashierName.trim().ifBlank { "Kasir 1" },
                            paperSize = paperSize,
                            headerFontSize = headerFontSize,
                            showAddress = showAddress,
                            showPhone = showPhone,
                            showCashier = showCashier,
                            showDateTime = showDateTime,
                            showFooter = showFooter
                        )
                        onUpdateStoreSettings(updated)
                        onShowMessage("Format dan template struk berhasil disimpan!")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_receipt_format_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Format & Template Struk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Section 4: Live Receipt Thermal Paper Simulation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pratinjau Hasil Cetakan (Real-Time)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tampilan struk fisik di bawah ini berubah seketika saat font atau baris template diubah.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                ThermalPaperView(
                    settings = previewSettings,
                    transaction = sampleTx,
                    items = sampleItems,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Reusable Card component for an individual receipt template line.
 * Provides active indicator and "Hapus Baris" / "+ Aktifkan Baris" action buttons.
 */
@Composable
private fun TemplateRowItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color(0xFFF1F5F9),
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else Color(0xFF64748B)
                    )
                }

                // Tombol Hapus Baris vs Aktifkan Baris
                if (isActive) {
                    OutlinedButton(
                        onClick = { onToggleActive(false) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Baris", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = { onToggleActive(true) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aktifkan Baris", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isActive) {
                content()
            } else {
                Text(
                    text = "Baris ini dihapus/disembunyikan dari struk cetak.",
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
