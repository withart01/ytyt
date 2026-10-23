package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_kasir.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial menu items in background
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.productDao()?.insertProducts(getInitialSampleProducts())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        fun getInitialSampleProducts(): List<ProductEntity> = listOf(
            ProductEntity(name = "Kopi Susu Gula Aren", category = "Minuman", price = 18000, stock = 50, colorHex = "#78350F"),
            ProductEntity(name = "Es Teh Manis", category = "Minuman", price = 5000, stock = 100, colorHex = "#B45309"),
            ProductEntity(name = "Matcha Latte", category = "Minuman", price = 22000, stock = 30, colorHex = "#15803D"),
            ProductEntity(name = "Es Jeruk Peras", category = "Minuman", price = 7000, stock = 60, colorHex = "#EA580C"),
            ProductEntity(name = "Air Mineral 600ml", category = "Minuman", price = 4000, stock = 80, colorHex = "#0284C7"),
            ProductEntity(name = "Nasi Goreng Spesial", category = "Makanan", price = 25000, stock = 40, colorHex = "#DC2626"),
            ProductEntity(name = "Mie Goreng Jawa", category = "Makanan", price = 22000, stock = 35, colorHex = "#D97706"),
            ProductEntity(name = "Ayam Bakar Madu", category = "Makanan", price = 28000, stock = 25, colorHex = "#B91C1C"),
            ProductEntity(name = "Ayam Geprek Komplit", category = "Makanan", price = 20000, stock = 30, colorHex = "#C2410C"),
            ProductEntity(name = "Kentang Goreng", category = "Snack", price = 15000, stock = 50, colorHex = "#CA8A04"),
            ProductEntity(name = "Pisang Bakar Keju", category = "Snack", price = 14000, stock = 40, colorHex = "#A16207"),
            ProductEntity(name = "Roti Bakar Coklat", category = "Snack", price = 16000, stock = 30, colorHex = "#854D0E"),
            ProductEntity(name = "Tahu Crispy Gurih", category = "Snack", price = 12000, stock = 45, colorHex = "#EAB308"),
            ProductEntity(name = "Paket Hemat Siang", category = "Paket", price = 30000, stock = 20, colorHex = "#0F766E")
        )
    }
}
