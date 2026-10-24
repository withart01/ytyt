package com.example.data.model

/**
 * Represents an active cashier shift / session.
 * Tracks session duration, cashier on duty, initial cash drawer balance,
 * and persistent storage status.
 */
data class CashierSession(
    val sessionId: String = "",
    val cashierName: String = "Kasir 1",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val initialCash: Long = 100000L, // Modal Awal di Laci Kasir (default Rp 100.000)
    val isActive: Boolean = true,
    val isPersistentStorageEnabled: Boolean = true
)
