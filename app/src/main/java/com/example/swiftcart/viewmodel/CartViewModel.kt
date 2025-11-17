package com.example.swiftcart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swiftcart.models.Cart
import com.example.swiftcart.models.Product
import com.example.swiftcart.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _cart = MutableStateFlow<Cart?>(null)
    val cart: StateFlow<Cart?> = _cart

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess

    fun loadCart() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.getCart()
            _isLoading.value = false

            if (result.isSuccess) {
                _cart.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load cart"
            }
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.addToCart(product, quantity)
            _isLoading.value = false

            if (result.isSuccess) {
                _operationSuccess.value = "${product.name} added to cart"
                loadCart()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to add to cart"
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        viewModelScope.launch {
            _error.value = null
            val result = repository.updateCartItemQuantity(productId, quantity)

            if (result.isSuccess) {
                loadCart()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update quantity"
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            _error.value = null
            val result = repository.removeFromCart(productId)

            if (result.isSuccess) {
                _operationSuccess.value = "Item removed from cart"
                loadCart()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to remove item"
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            _error.value = null
            val result = repository.clearCart()

            if (result.isSuccess) {
                loadCart()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to clear cart"
            }
        }
    }

    fun placeOrder() {
        val currentCart = _cart.value ?: return
        if (currentCart.items.isEmpty()) {
            _error.value = "Cart is empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.createOrder(currentCart)
            _isLoading.value = false

            if (result.isSuccess) {
                _operationSuccess.value = "Order placed successfully! Order ID: ${result.getOrNull()}"
                loadCart()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to place order"
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _operationSuccess.value = null
    }
}