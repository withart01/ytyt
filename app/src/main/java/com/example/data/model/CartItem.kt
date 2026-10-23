package com.example.data.model

data class CartItem(
    val productId: Long? = null,
    val name: String,
    val price: Long,
    val quantity: Int = 1,
    val note: String = ""
) {
    val total: Long get() = price * quantity
}
