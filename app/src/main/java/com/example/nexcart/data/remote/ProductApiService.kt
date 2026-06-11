package com.example.nexcart.data.remote

import com.example.nexcart.domain.model.Product
import retrofit2.http.GET

interface ProductApiService {
    @GET("products")
    suspend fun getProducts(): List<Product>
}
