package com.example.data.model

data class StoreSettings(
    val storeName: String = "PT SARASA FOOD",
    val address: String = "Jl. Sudirman No. 128, Indonesia",
    val phone: String = "0812-3456-7890",
    val footerMessage: String = "Terima kasih atas kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar.",
    val paperSize: Int = 58, // 58mm (32 chars) or 80mm (48 chars)
    val cashierName: String = "Kasir 1",
    val selectedPrinterMac: String = "",
    val selectedPrinterName: String = "",
    // Opsi Ukuran Font & Baris Template
    val headerFontSize: String = "BESAR", // "BESAR" (2x2), "SEDANG" (1x2 tall), "NORMAL" (1x1)
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    val showCashier: Boolean = true,
    val showDateTime: Boolean = true,
    val showFooter: Boolean = true
)
