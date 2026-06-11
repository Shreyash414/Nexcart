package com.example.nexcart.domain.repository

import com.example.nexcart.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    // Cloud/Remote Products
    suspend fun getProducts(): List<Product>
    suspend fun getProductsBySeller(sellerId: String): List<Product>
    suspend fun addProduct(product: Product): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>

    // Recommended Products (API)
    suspend fun getRecommendedProducts(): List<Product>

    // Favorites (Room)
    fun getFavoriteProducts(): Flow<List<Product>>
    suspend fun addFavorite(product: Product)
    suspend fun removeFavorite(product: Product)
    suspend fun isFavorite(productId: String): Boolean
}
