package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight JSON serializer for CartItem list using Android built-in org.json.
 * Zero external reflection, minimal memory overhead for low-end devices like Samsung Galaxy J2.
 */
object CartJsonHelper {

    fun toJson(items: List<CartItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            item.productId?.let { obj.put("id", it) }
            obj.put("name", item.name)
            obj.put("price", item.price)
            obj.put("quantity", item.quantity)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<CartItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<CartItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = if (obj.has("id")) obj.getLong("id") else null
                val name = obj.optString("name", "Item")
                val price = obj.optLong("price", 0L)
                val quantity = obj.optInt("quantity", 1)
                list.add(
                    CartItem(
                        productId = id,
                        name = name,
                        price = price,
                        quantity = quantity
                    )
                )
            }
        } catch (_: Exception) {
            // Return empty list on parse error
        }
        return list
    }
}
