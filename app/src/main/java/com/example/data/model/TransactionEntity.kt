package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Long,
    val discount: Long = 0,
    val tax: Long = 0,
    val grandTotal: Long,
    val paymentMethod: String, // Tunai, QRIS, Transfer, Debit
    val cashReceived: Long = 0,
    val changeAmount: Long = 0,
    val cashierName: String = "Kasir 1",
    val customerName: String = "",
    val itemsSummary: String, // e.g. "Kopi Susu (2x), Roti Bakar (1x)"
    val itemsJson: String    // detailed serialized list
)
