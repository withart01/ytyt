package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // e.g. Makanan, Minuman, Snack, Jasa, Lainnya
    val price: Long,      // In Rupiah, e.g. 15000
    val stock: Int = 100,
    val barcode: String = "",
    val colorHex: String = "#0F766E"
)
