package com.example.nexcart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.model.Rating

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val image: String,
    val images: List<String> = emptyList(),
    val rate: Double,
    val ratingCount: Int,
    val sellerId: String
) {
    fun toDomain(): Product {
        return Product(
            id = id,
            title = title,
            price = price,
            description = description,
            category = category,
            image = image,
            images = images,
            rating = Rating(rate = rate, count = ratingCount),
            sellerId = sellerId
        )
    }

    companion object {
        fun fromDomain(product: Product): ProductEntity {
            return ProductEntity(
                id = product.id,
                title = product.title,
                price = product.price,
                description = product.description,
                category = product.category,
                image = product.image,
                images = product.images,
                rate = product.rating.rate,
                ratingCount = product.rating.count,
                sellerId = product.sellerId
            )
        }
    }
}
