package com.example.swiftcart.models

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val stock: Int = 0,
    val category: String = ""
)

data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
)

data class Cart(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun getTotalPrice(): Double = items.sumOf { it.price * it.quantity }
    fun getTotalItems(): Int = items.sumOf { it.quantity }
}

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "Pending",
    val createdAt: Timestamp = Timestamp.now()
)