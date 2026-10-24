package com.example.printer

import com.example.data.model.CartItem
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPosHelper {

    // ESC/POS Command Constants
    val CMD_INIT = byteArrayOf(0x1B, 0x40)
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    val CMD_TEXT_SIZE_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    val CMD_TEXT_SIZE_LARGE = byteArrayOf(0x1D, 0x21, 0x11) // 2x width, 2x height
    val CMD_TEXT_SIZE_TALL = byteArrayOf(0x1D, 0x21, 0x01) // 2x height
    val CMD_FEED_AND_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
    val LF = byteArrayOf(0x0A)

    private val localeId = Locale("id", "ID")
    private val rupiahFormat = NumberFormat.getCurrencyInstance(localeId).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", localeId)

    @Synchronized
    fun formatRupiah(amount: Long): String {
        return rupiahFormat.format(amount).replace("Rp", "Rp ")
    }

    @Synchronized
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Formats two columns (Left aligned and Right aligned) to fit exact paper width.
     */
    fun formatTwoColumns(left: String, right: String, width: Int): String {
        val totalLength = left.length + right.length
        return if (totalLength <= width) {
            val spaces = " ".repeat(width - totalLength)
            left + spaces + right
        } else {
            // Left is too long, truncate or wrap
            val maxLeft = width - right.length - 1
            if (maxLeft > 0) {
                left.take(maxLeft) + " " + right
            } else {
                left + "\n" + " ".repeat(width - right.length) + right
            }
        }
    }

    /**
     * Builds standard ESC/POS binary data for a complete transaction receipt.
     */
    fun buildReceiptBytes(
        settings: StoreSettings,
        transaction: TransactionEntity,
        items: List<CartItem>
    ): ByteArray {
        val width = if (settings.paperSize == 80) 48 else 32
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))
        fun writeLine(text: String = "") {
            writeText(text)
            write(LF)
        }

        // 1. Initialize printer
        write(CMD_INIT)

        // 2. Header (Store Name centered, large & bold)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_TEXT_SIZE_LARGE)
        writeLine(settings.storeName)

        write(CMD_BOLD_OFF)
        write(CMD_TEXT_SIZE_NORMAL)
        if (settings.address.isNotBlank()) writeLine(settings.address)
        if (settings.phone.isNotBlank()) writeLine("Telp: ${settings.phone}")
        writeLine("-".repeat(width))

        // 3. Metadata (No. Struk, Kasir, Tanggal)
        write(CMD_ALIGN_LEFT)
        writeLine("No: ${transaction.invoiceNumber}")
        writeLine("Tgl: ${formatDate(transaction.timestamp)}")
        val cashierInfo = "Kasir: ${transaction.cashierName}"
        val customerInfo = if (transaction.customerName.isNotBlank()) "Plg: ${transaction.customerName}" else ""
        if (customerInfo.isNotBlank()) {
            writeLine(formatTwoColumns(cashierInfo, customerInfo, width))
        } else {
            writeLine(cashierInfo)
        }
        writeLine("-".repeat(width))

        // 4. Itemized list
        items.forEach { item ->
            writeLine(item.name)
            val qtyPrice = "  ${item.quantity}x @${formatRupiah(item.price)}"
            val lineTotal = formatRupiah(item.total)
            writeLine(formatTwoColumns(qtyPrice, lineTotal, width))
            if (item.note.isNotBlank()) {
                writeLine("  (${item.note})")
            }
        }
        writeLine("-".repeat(width))

        // 5. Totals
        write(CMD_ALIGN_LEFT)
        writeLine(formatTwoColumns("Subtotal:", formatRupiah(transaction.subtotal), width))

        if (transaction.discount > 0) {
            writeLine(formatTwoColumns("Diskon:", "-${formatRupiah(transaction.discount)}", width))
        }
        if (transaction.tax > 0) {
            writeLine(formatTwoColumns("PPN (11%):", formatRupiah(transaction.tax), width))
        }

        write(CMD_BOLD_ON)
        writeLine(formatTwoColumns("TOTAL:", formatRupiah(transaction.grandTotal), width))
        write(CMD_BOLD_OFF)

        writeLine(formatTwoColumns("Metode Bayar:", transaction.paymentMethod, width))
        if (transaction.paymentMethod.equals("Tunai", ignoreCase = true)) {
            writeLine(formatTwoColumns("Tunai Diterima:", formatRupiah(transaction.cashReceived), width))
            write(CMD_BOLD_ON)
            writeLine(formatTwoColumns("Kembalian:", formatRupiah(transaction.changeAmount), width))
            write(CMD_BOLD_OFF)
        }
        writeLine("-".repeat(width))

        // 6. Footer Note
        write(CMD_ALIGN_CENTER)
        settings.footerMessage.lines().forEach { line ->
            if (line.isNotBlank()) writeLine(line)
        }

        // 7. Feed paper so receipt can be cut/torn
        writeLine("")
        writeLine("")
        writeLine("")
        write(CMD_FEED_AND_CUT)

        return stream.toByteArray()
    }

    /**
     * Builds ESC/POS test receipt bytes to check printer connectivity.
     */
    fun buildTestReceiptBytes(settings: StoreSettings): ByteArray {
        val width = if (settings.paperSize == 80) 48 else 32
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))
        fun writeLine(text: String = "") {
            writeText(text)
            write(LF)
        }

        write(CMD_INIT)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_TEXT_SIZE_LARGE)
        writeLine("TEST PRINTER")
        write(CMD_BOLD_OFF)
        write(CMD_TEXT_SIZE_NORMAL)
        writeLine(settings.storeName)
        writeLine("Lebar Kertas: ${settings.paperSize}mm ($width Kolom)")
        writeLine(formatDate(System.currentTimeMillis()))
        writeLine("=".repeat(width))

        write(CMD_ALIGN_LEFT)
        writeLine("Koneksi Bluetooth Berhasil!")
        writeLine(formatTwoColumns("Kiri", "Kanan", width))
        write(CMD_BOLD_ON)
        writeLine(formatTwoColumns("Teks Tebal", "OK", width))
        write(CMD_BOLD_OFF)
        writeLine("-".repeat(width))
        write(CMD_ALIGN_CENTER)
        writeLine("Printer Siap Digunakan.")
        writeLine("")
        writeLine("")
        writeLine("")
        write(CMD_FEED_AND_CUT)

        return stream.toByteArray()
    }

    /**
     * Builds ESC/POS bytes for daily sales summary report.
     */
    fun buildDailyReportBytes(
        settings: StoreSettings,
        dateText: String,
        totalSales: Long,
        txCount: Int,
        cashTotal: Long,
        nonCashTotal: Long
    ): ByteArray {
        val width = if (settings.paperSize == 80) 48 else 32
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))
        fun writeLine(text: String = "") {
            writeText(text)
            write(LF)
        }

        write(CMD_INIT)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_TEXT_SIZE_LARGE)
        writeLine("LAPORAN PENJUALAN")
        write(CMD_BOLD_OFF)
        write(CMD_TEXT_SIZE_NORMAL)
        writeLine(settings.storeName)
        writeLine("Periode: $dateText")
        writeLine("Dicetak: ${formatDate(System.currentTimeMillis())}")
        writeLine("=".repeat(width))

        write(CMD_ALIGN_LEFT)
        writeLine(formatTwoColumns("Jumlah Transaksi:", "$txCount Struk", width))
        write(CMD_BOLD_ON)
        writeLine(formatTwoColumns("TOTAL OMZET:", formatRupiah(totalSales), width))
        write(CMD_BOLD_OFF)
        writeLine("-".repeat(width))
        writeLine(formatTwoColumns("Pembayaran Tunai:", formatRupiah(cashTotal), width))
        writeLine(formatTwoColumns("Non-Tunai (QRIS/Trf):", formatRupiah(nonCashTotal), width))
        writeLine("-".repeat(width))

        write(CMD_ALIGN_CENTER)
        writeLine("POS Kasir Android")
        writeLine("")
        writeLine("")
        writeLine("")
        write(CMD_FEED_AND_CUT)

        return stream.toByteArray()
    }

    /**
     * Builds ESC/POS bytes for Cashier Shift / Session Closing report (Z-Report).
     */
    fun buildCashierSessionReportBytes(
        settings: StoreSettings,
        session: com.example.data.model.CashierSession,
        totalSales: Long,
        txCount: Int,
        cashTotal: Long,
        nonCashTotal: Long
    ): ByteArray {
        val width = if (settings.paperSize == 80) 48 else 32
        val stream = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = stream.write(bytes)
        fun writeText(text: String) = stream.write(text.toByteArray(Charsets.ISO_8859_1))
        fun writeLine(text: String = "") {
            writeText(text)
            write(LF)
        }

        write(CMD_INIT)
        write(CMD_ALIGN_CENTER)
        write(CMD_BOLD_ON)
        write(CMD_TEXT_SIZE_LARGE)
        writeLine("REKAP SHIFT KASIR")
        write(CMD_BOLD_OFF)
        write(CMD_TEXT_SIZE_NORMAL)
        writeLine(settings.storeName)
        writeLine("Sesi: ${session.sessionId}")
        writeLine("Kasir: ${session.cashierName}")
        writeLine("Mulai: ${formatDate(session.startTime)}")
        session.endTime?.let { writeLine("Selesai: ${formatDate(it)}") }
        writeLine("=".repeat(width))

        write(CMD_ALIGN_LEFT)
        writeLine(formatTwoColumns("Modal Awal Laci:", formatRupiah(session.initialCash), width))
        writeLine(formatTwoColumns("Jml Transaksi:", "$txCount Struk", width))
        writeLine(formatTwoColumns("Penjualan Tunai:", formatRupiah(cashTotal), width))
        writeLine(formatTwoColumns("Penjualan Non-Tunai:", formatRupiah(nonCashTotal), width))
        writeLine("-".repeat(width))
        write(CMD_BOLD_ON)
        writeLine(formatTwoColumns("TOTAL OMZET SESI:", formatRupiah(totalSales), width))
        writeLine(formatTwoColumns("TOTAL UANG FISIK LACI:", formatRupiah(session.initialCash + cashTotal), width))
        write(CMD_BOLD_OFF)
        writeLine("=".repeat(width))

        write(CMD_ALIGN_CENTER)
        writeLine("Status: Penyimpanan Permanen Aktif")
        writeLine("Laporan Kasir Selesai")
        writeLine("")
        writeLine("")
        writeLine("")
        write(CMD_FEED_AND_CUT)

        return stream.toByteArray()
    }


    /**
     * Builds clean formatted string representation for on-screen thermal paper preview.
     */
    fun buildReceiptText(
        settings: StoreSettings,
        transaction: TransactionEntity,
        items: List<CartItem>
    ): String {
        val width = if (settings.paperSize == 80) 48 else 32
        val sb = StringBuilder()

        fun center(text: String) {
            val pad = ((width - text.length) / 2).coerceAtLeast(0)
            sb.append(" ".repeat(pad)).append(text).append("\n")
        }

        center(settings.storeName.uppercase())
        if (settings.address.isNotBlank()) center(settings.address)
        if (settings.phone.isNotBlank()) center("Telp: ${settings.phone}")
        sb.append("-".repeat(width)).append("\n")

        sb.append("No: ${transaction.invoiceNumber}").append("\n")
        sb.append("Tgl: ${formatDate(transaction.timestamp)}").append("\n")
        sb.append("Kasir: ${transaction.cashierName}").append("\n")
        if (transaction.customerName.isNotBlank()) {
            sb.append("Pelanggan: ${transaction.customerName}").append("\n")
        }
        sb.append("-".repeat(width)).append("\n")

        items.forEach { item ->
            sb.append(item.name).append("\n")
            val qty = "  ${item.quantity}x @${formatRupiah(item.price)}"
            val lineTotal = formatRupiah(item.total)
            sb.append(formatTwoColumns(qty, lineTotal, width)).append("\n")
        }
        sb.append("-".repeat(width)).append("\n")

        sb.append(formatTwoColumns("Subtotal:", formatRupiah(transaction.subtotal), width)).append("\n")
        if (transaction.discount > 0) {
            sb.append(formatTwoColumns("Diskon:", "-${formatRupiah(transaction.discount)}", width)).append("\n")
        }
        if (transaction.tax > 0) {
            sb.append(formatTwoColumns("PPN (11%):", formatRupiah(transaction.tax), width)).append("\n")
        }
        sb.append(formatTwoColumns("TOTAL:", formatRupiah(transaction.grandTotal), width)).append("\n")
        sb.append(formatTwoColumns("Bayar (${transaction.paymentMethod}):", formatRupiah(transaction.cashReceived.takeIf { it > 0 } ?: transaction.grandTotal), width)).append("\n")
        if (transaction.paymentMethod.equals("Tunai", ignoreCase = true)) {
            sb.append(formatTwoColumns("Kembali:", formatRupiah(transaction.changeAmount), width)).append("\n")
        }
        sb.append("-".repeat(width)).append("\n")

        settings.footerMessage.lines().forEach { line ->
            if (line.isNotBlank()) center(line)
        }

        return sb.toString()
    }
}
