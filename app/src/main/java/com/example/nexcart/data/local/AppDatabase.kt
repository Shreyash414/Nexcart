package com.example.nexcart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nexcart.data.local.dao.FavoriteDao
import com.example.nexcart.data.local.dao.ProductDao
import com.example.nexcart.data.local.entity.FavoriteProductEntity
import com.example.nexcart.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, FavoriteProductEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun favoriteDao(): FavoriteDao
}
