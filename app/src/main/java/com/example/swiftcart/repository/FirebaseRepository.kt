package com.example.swiftcart.repository

import android.util.Log
import com.example.swiftcart.models.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val TAG = "FirebaseRepository"

    // Auth Methods
    fun getCurrentUser() = auth.currentUser

    suspend fun signUpWithEmail(email: String, password: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Attempting sign up for: $email")
            auth.createUserWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Sign up successful")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Attempting sign in for: $email")
            auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Sign in successful")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Attempting Google sign in")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Log.d(TAG, "Google sign in successful")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    // Product Methods (READ)
    suspend fun getProducts(): Result<List<Product>> {
        return try {
            Log.d(TAG, "Fetching products from Firestore")
            val snapshot = firestore.collection("products")
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                try {
                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        category = doc.getString("category") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing product: ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "Fetched ${products.size} products")
            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch products", e)
            Result.failure(e)
        }
    }

    suspend fun getProduct(productId: String): Result<Product?> {
        return try {
            Log.d(TAG, "Fetching product: $productId")
            val doc = firestore.collection("products")
                .document(productId)
                .get()
                .await()

            val product = if (doc.exists()) {
                Product(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    stock = doc.getLong("stock")?.toInt() ?: 0,
                    category = doc.getString("category") ?: ""
                )
            } else null

            Log.d(TAG, "Product fetched: ${product?.name}")
            Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch product", e)
            Result.failure(e)
        }
    }

    // Cart Methods (CREATE & READ)
    suspend fun getCart(): Result<Cart> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Fetching cart for user: $userId")
            val doc = firestore.collection("carts")
                .document(userId)
                .get()
                .await()

            val cart = if (doc.exists()) {
                val itemsList = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                    try {
                        CartItem(
                            productId = item["productId"] as? String ?: "",
                            productName = item["productName"] as? String ?: "",
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                            imageUrl = item["imageUrl"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing cart item", e)
                        null
                    }
                } ?: emptyList()

                Cart(
                    id = doc.id,
                    userId = userId,
                    items = itemsList,
                    updatedAt = doc.getTimestamp("updatedAt") ?: Timestamp.now()
                )
            } else {
                Cart(id = userId, userId = userId)
            }

            Log.d(TAG, "Cart fetched: ${cart.items.size} items")
            Result.success(cart)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch cart", e)
            Result.failure(e)
        }
    }

    suspend fun addToCart(product: Product, quantity: Int = 1): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Adding to cart: ${product.name} x$quantity")
            val cartRef = firestore.collection("carts").document(userId)
            val doc = cartRef.get().await()

            val existingItems = if (doc.exists()) {
                (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                    try {
                        CartItem(
                            productId = item["productId"] as? String ?: "",
                            productName = item["productName"] as? String ?: "",
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                            imageUrl = item["imageUrl"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.toMutableList() ?: mutableListOf()
            } else {
                mutableListOf()
            }

            val existingItemIndex = existingItems.indexOfFirst { it.productId == product.id }

            if (existingItemIndex != -1) {
                val item = existingItems[existingItemIndex]
                existingItems[existingItemIndex] = item.copy(quantity = item.quantity + quantity)
            } else {
                existingItems.add(
                    CartItem(
                        productId = product.id,
                        productName = product.name,
                        price = product.price,
                        quantity = quantity,
                        imageUrl = product.imageUrl
                    )
                )
            }

            val cartData = hashMapOf(
                "userId" to userId,
                "items" to existingItems.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "price" to item.price,
                        "quantity" to item.quantity,
                        "imageUrl" to item.imageUrl
                    )
                },
                "updatedAt" to Timestamp.now()
            )

            cartRef.set(cartData).await()
            Log.d(TAG, "Added to cart successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to cart", e)
            Result.failure(e)
        }
    }

    suspend fun updateCartItemQuantity(productId: String, quantity: Int): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Updating cart quantity: $productId = $quantity")
            val cartRef = firestore.collection("carts").document(userId)
            val doc = cartRef.get().await()

            if (!doc.exists()) return Result.failure(Exception("Cart not found"))

            val items = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                try {
                    CartItem(
                        productId = item["productId"] as? String ?: "",
                        productName = item["productName"] as? String ?: "",
                        price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                        quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                        imageUrl = item["imageUrl"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            val updatedItems = items.map { item ->
                if (item.productId == productId) item.copy(quantity = quantity) else item
            }.filter { it.quantity > 0 }

            val cartData = hashMapOf(
                "userId" to userId,
                "items" to updatedItems.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "price" to item.price,
                        "quantity" to item.quantity,
                        "imageUrl" to item.imageUrl
                    )
                },
                "updatedAt" to Timestamp.now()
            )

            cartRef.set(cartData).await()
            Log.d(TAG, "Cart quantity updated")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update cart", e)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(productId: String): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Removing from cart: $productId")
            val cartRef = firestore.collection("carts").document(userId)
            val doc = cartRef.get().await()

            if (!doc.exists()) return Result.failure(Exception("Cart not found"))

            val items = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                try {
                    CartItem(
                        productId = item["productId"] as? String ?: "",
                        productName = item["productName"] as? String ?: "",
                        price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                        quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                        imageUrl = item["imageUrl"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            val updatedItems = items.filter { it.productId != productId }

            val cartData = hashMapOf(
                "userId" to userId,
                "items" to updatedItems.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "price" to item.price,
                        "quantity" to item.quantity,
                        "imageUrl" to item.imageUrl
                    )
                },
                "updatedAt" to Timestamp.now()
            )

            cartRef.set(cartData).await()
            Log.d(TAG, "Item removed from cart")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove from cart", e)
            Result.failure(e)
        }
    }

    suspend fun clearCart(): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Clearing cart")
            val cartData = hashMapOf(
                "userId" to userId,
                "items" to emptyList<Map<String, Any>>(),
                "updatedAt" to Timestamp.now()
            )
            firestore.collection("carts").document(userId).set(cartData).await()
            Log.d(TAG, "Cart cleared")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cart", e)
            Result.failure(e)
        }
    }

    // Order Methods (CREATE & READ)
    suspend fun createOrder(cart: Cart): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Creating order")
            val orderData = hashMapOf(
                "userId" to userId,
                "items" to cart.items.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "price" to item.price,
                        "quantity" to item.quantity,
                        "imageUrl" to item.imageUrl
                    )
                },
                "totalAmount" to cart.getTotalPrice(),
                "status" to "Pending",
                "createdAt" to Timestamp.now()
            )

            val orderRef = firestore.collection("orders").document()
            orderRef.set(orderData).await()

            // Clear cart after order
            clearCart()

            Log.d(TAG, "Order created: ${orderRef.id}")
            Result.success(orderRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create order", e)
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<Order>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            Log.d(TAG, "Fetching orders")
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull { doc ->
                try {
                    val items = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                        CartItem(
                            productId = item["productId"] as? String ?: "",
                            productName = item["productName"] as? String ?: "",
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                            imageUrl = item["imageUrl"] as? String ?: ""
                        )
                    } ?: emptyList()

                    Order(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        items = items,
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        status = doc.getString("status") ?: "Pending",
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing order: ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "Fetched ${orders.size} orders")
            Result.success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch orders", e)
            Result.failure(e)
        }
    }
}