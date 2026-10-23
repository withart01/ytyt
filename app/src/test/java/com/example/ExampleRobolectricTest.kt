package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CartItem
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.printer.EscPosHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("POS Kasir", appName)
    }

    @Test
    fun `test esc pos receipt generation`() {
        val settings = StoreSettings(storeName = "KASIR KITA POS")
        val tx = TransactionEntity(
            invoiceNumber = "INV-20261023001",
            subtotal = 35000,
            grandTotal = 35000,
            paymentMethod = "Tunai",
            cashReceived = 50000,
            changeAmount = 15000,
            itemsSummary = "Kopi Susu (1x), Nasi Goreng (1x)",
            itemsJson = ""
        )
        val items = listOf(
            CartItem(name = "Kopi Susu", price = 15000, quantity = 1),
            CartItem(name = "Nasi Goreng", price = 20000, quantity = 1)
        )

        val bytes = EscPosHelper.buildReceiptBytes(settings, tx, items)
        assertTrue(bytes.isNotEmpty())

        val text = EscPosHelper.buildReceiptText(settings, tx, items)
        assertTrue(text.contains("KASIR KITA POS"))
        assertTrue(text.contains("INV-20261023001"))
        assertTrue(text.contains("Kopi Susu"))
    }
}
