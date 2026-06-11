package com.example.nexcart.data.api

import com.example.nexcart.data.model.ProductDto
import retrofit2.http.GET

interface FakeStoreApi {

    @GET("products")
    suspend fun getProducts(): List<ProductDto>
}