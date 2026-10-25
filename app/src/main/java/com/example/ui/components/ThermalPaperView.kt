package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.printer.EscPosHelper

@Composable
fun ThermalPaperView(
    settings: StoreSettings,
    transaction: TransactionEntity,
    items: List<CartItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("thermal_paper_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFDF5) // Authentic off-white receipt paper tone
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Store
            val (headerFontSize, headerFontWeight) = when (settings.headerFontSize) {
                "SEDANG" -> Pair(15.sp, FontWeight.Bold)
                "NORMAL" -> Pair(13.sp, FontWeight.SemiBold)
                else -> Pair(19.sp, FontWeight.Bold) // "BESAR"
            }

            Text(
                text = settings.storeName.uppercase(),
                fontWeight = headerFontWeight,
                fontSize = headerFontSize,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )

            if (settings.showAddress && settings.address.isNotBlank()) {
                Text(
                    text = settings.address,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (settings.showPhone && settings.phone.isNotBlank()) {
                Text(
                    text = "Telp: ${settings.phone}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
            }

            DashedReceiptDivider()

            // Metadata
            ReceiptRow("No. Struk", transaction.invoiceNumber)
            if (settings.showDateTime) {
                ReceiptRow("Tanggal", EscPosHelper.formatDate(transaction.timestamp))
            }
            if (settings.showCashier) {
                ReceiptRow("Kasir", transaction.cashierName)
            }
            if (transaction.customerName.isNotBlank()) {
                ReceiptRow("Pelanggan", transaction.customerName)
            }

            DashedReceiptDivider()

            // Items
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = item.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    ReceiptRow(
                        left = "  ${item.quantity}x @${EscPosHelper.formatRupiah(item.price)}",
                        right = EscPosHelper.formatRupiah(item.total),
                        isItemRow = true
                    )
                    if (item.note.isNotBlank()) {
                        Text(
                            text = "  (${item.note})",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            DashedReceiptDivider()

            // Subtotal & Totals
            ReceiptRow("Subtotal", EscPosHelper.formatRupiah(transaction.subtotal))
            if (transaction.discount > 0) {
                ReceiptRow("Diskon", "-${EscPosHelper.formatRupiah(transaction.discount)}", isHighlight = true)
            }
            if (transaction.tax > 0) {
                ReceiptRow("PPN (11%)", EscPosHelper.formatRupiah(transaction.tax))
            }

            ReceiptRow("TOTAL", EscPosHelper.formatRupiah(transaction.grandTotal), isTotal = true)

            DashedReceiptDivider()

            ReceiptRow("Metode Bayar", transaction.paymentMethod)
            if (transaction.paymentMethod.equals("Tunai", ignoreCase = true)) {
                ReceiptRow("Tunai", EscPosHelper.formatRupiah(transaction.cashReceived))
                ReceiptRow("Kembalian", EscPosHelper.formatRupiah(transaction.changeAmount), isTotal = true)
            }

            // Footer
            if (settings.showFooter && settings.footerMessage.isNotBlank()) {
                DashedReceiptDivider()
                settings.footerMessage.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    left: String,
    right: String,
    isTotal: Boolean = false,
    isItemRow: Boolean = false,
    isHighlight: Boolean = false
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isTotal) 4.dp else 1.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = left,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isTotal) 14.sp else 12.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color(0xFF0F172A) else Color(0xFF334155)
        )
        Text(
            text = right,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isTotal) 15.sp else 12.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) Color(0xFFDC2626) else if (isTotal) Color(0xFF0F766E) else Color(0xFF334155)
        )
    }
}

@Composable
private fun DashedReceiptDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = Color(0xFF94A3B8),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}
