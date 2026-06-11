package com.example.nexcart.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexcart.data.local.entity.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId")
    fun getProductsBySeller(sellerId: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProduct(product: ProductEntity): Long

    @Update
    fun updateProduct(product: ProductEntity): Int

    @Query("DELETE FROM products WHERE id = :productId")
    fun deleteProduct(productId: String): Int
}
