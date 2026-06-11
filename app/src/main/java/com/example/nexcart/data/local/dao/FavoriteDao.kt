package com.example.nexcart.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nexcart.data.local.entity.FavoriteProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_products")
    fun getAllFavorites(): Flow<List<FavoriteProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFavorite(product: FavoriteProductEntity): Long

    @Delete
    fun removeFavorite(product: FavoriteProductEntity): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE id = :productId)")
    fun isFavorite(productId: String): Boolean
}
