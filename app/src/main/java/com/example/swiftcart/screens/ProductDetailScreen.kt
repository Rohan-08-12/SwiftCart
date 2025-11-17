package com.example.swiftcart.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.swiftcart.viewmodel.CartViewModel
import com.example.swiftcart.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    productViewModel: ProductViewModel,
    cartViewModel: CartViewModel,
    onBackClick: () -> Unit
) {
    val product by productViewModel.selectedProduct.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val operationSuccess by cartViewModel.operationSuccess.collectAsState()
    val context = LocalContext.current

    var quantity by remember { mutableStateOf(1) }

    LaunchedEffect(productId) {
        productViewModel.selectProduct(productId)
    }

    LaunchedEffect(operationSuccess) {
        operationSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            cartViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                product == null -> {
                    Text(
                        text = "Product not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Product Image
                        AsyncImage(
                            model = product!!.imageUrl.ifBlank { "https://via.placeholder.com/400" },
                            contentDescription = product!!.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Product Name & Price
                        Text(
                            text = product!!.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$${String.format("%.2f", product!!.price)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (product!!.stock > 0)
                                "In Stock (${product!!.stock} available)"
                            else
                                "Out of Stock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (product!!.stock > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = product!!.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Quantity Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quantity:",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-")
                            }

                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Button(
                                onClick = { if (quantity < product!!.stock) quantity++ },
                                enabled = quantity < product!!.stock,
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add to Cart Button
                        Button(
                            onClick = {
                                product?.let { cartViewModel.addToCart(it, quantity) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = product!!.stock > 0
                        ) {
                            Text(
                                text = if (product!!.stock > 0) "Add to Cart" else "Out of Stock",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}