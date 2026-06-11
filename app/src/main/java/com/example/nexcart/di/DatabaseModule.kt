package com.example.nexcart.di

import android.content.Context
import androidx.room.Room
import com.example.nexcart.data.local.AppDatabase
import com.example.nexcart.data.local.dao.FavoriteDao
import com.example.nexcart.data.local.dao.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nexcart_db"
        )
        .fallbackToDestructiveMigration() // Adding this since we changed version
        .build()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }
}
