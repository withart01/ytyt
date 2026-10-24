package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.CartJsonHelper
import com.example.data.model.CashierSession
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.printer.EscPosHelper
import com.example.printer.PrinterStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    storeSettings: StoreSettings,
    cashierSession: CashierSession,
    printerStatus: PrinterStatus,
    onPrintDailyReport: (dateText: String, total: Long, count: Int, cash: Long, nonCash: Long) -> Unit,
    onRePrintReceipt: (TransactionEntity, List<CartItem>) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onStartNewSession: (cashierName: String, initialCash: Long) -> Unit,
    onCloseSession: () -> Unit,
    onPrintSessionReport: () -> Unit,
    onExportData: () -> String,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTxForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSessionDialog by remember { mutableStateOf(false) }

    val todaySdf = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }
    val todayText = remember { todaySdf.format(Date()) }

    // Summary stats
    val totalOmzet = transactions.sumOf { it.grandTotal }
    val totalCount = transactions.size
    val cashTotal = transactions.filter { it.paymentMethod.equals("Tunai", ignoreCase = true) }.sumOf { it.grandTotal }
    val nonCashTotal = totalOmzet - cashTotal

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("history_screen")
    ) {
        // Storage Status & Session Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showSessionDialog = true }
                .testTag("storage_status_bar"),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Mode Penyimpanan: SQLite AKTIF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "Sesi: ${cashierSession.cashierName} • ${transactions.size} transaksi tersimpan",
                            fontSize = 10.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF16A34A)
                ) {
                    Text(
                        text = "Kelola Sesi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Daily Sales Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Column {
                        Text(
                            text = "Ringkasan Penjualan",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = todayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = {
                            onPrintDailyReport(todayText, totalOmzet, totalCount, cashTotal, nonCashTotal)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("print_daily_report_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Laporan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = EscPosHelper.formatRupiah(totalOmzet),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Jumlah Transaksi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$totalCount Struk", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column {
                        Text("Tunai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(EscPosHelper.formatRupiah(cashTotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column {
                        Text("Non-Tunai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(EscPosHelper.formatRupiah(nonCashTotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Transaksi (${transactions.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Tersimpan Permanen",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Belum ada transaksi tercatat",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transaksi baru akan otomatis tersimpan permanen di memori perangkat ini.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onClick = { selectedTxForDetail = tx },
                        onPrint = {
                            val items = resolveTransactionItems(tx)
                            onRePrintReceipt(tx, items)
                        },
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }

    // Detail Receipt Dialog
    if (selectedTxForDetail != null) {
        val tx = selectedTxForDetail!!
        val items = resolveTransactionItems(tx)
        ReceiptDialog(
            transaction = tx,
            items = items,
            settings = storeSettings,
            printerStatus = printerStatus,
            onPrint = { onRePrintReceipt(tx, items) },
            onDismiss = { selectedTxForDetail = null }
        )
    }

    // Cashier Session Management Dialog
    if (showSessionDialog) {
        CashierSessionDialog(
            session = cashierSession,
            transactions = transactions,
            onStartNewSession = onStartNewSession,
            onCloseSession = onCloseSession,
            onPrintSessionReport = onPrintSessionReport,
            onExportData = onExportData,
            onShowMessage = onShowMessage,
            onDismiss = { showSessionDialog = false }
        )
    }
}

@Composable
private fun TransactionItemCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = EscPosHelper.formatDate(transaction.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = transaction.itemsSummary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = EscPosHelper.formatRupiah(transaction.grandTotal),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = transaction.paymentMethod,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row {
                    IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Cetak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Resolves exact transaction items from persistent JSON, or falls back to summary reconstruction.
 */
fun resolveTransactionItems(tx: TransactionEntity): List<CartItem> {
    if (tx.itemsJson.isNotBlank()) {
        val decoded = CartJsonHelper.fromJson(tx.itemsJson)
        if (decoded.isNotEmpty()) return decoded
    }
    return parseItemsSummaryToCartItems(tx.itemsSummary, tx.subtotal)
}

/**
 * Fallback parser reconstructing CartItems from summary text.
 */
fun parseItemsSummaryToCartItems(summary: String, subtotal: Long): List<CartItem> {
    if (summary.isBlank()) {
        return listOf(CartItem(name = "Total Pesanan", price = subtotal, quantity = 1))
    }
    val parts = summary.split(", ")
    return parts.mapNotNull { part ->
        val match = Regex("""^(.*)\s*\((\d+)x\)$""").find(part.trim())
        if (match != null) {
            val name = match.groupValues[1]
            val qty = match.groupValues[2].toIntOrNull() ?: 1
            CartItem(name = name, price = subtotal / (parts.size.coerceAtLeast(1) * qty), quantity = qty)
        } else {
            CartItem(name = part.trim(), price = subtotal / parts.size.coerceAtLeast(1), quantity = 1)
        }
    }
}
