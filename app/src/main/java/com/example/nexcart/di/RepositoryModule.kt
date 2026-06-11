package com.example.nexcart.di

import com.example.nexcart.data.repository.ProductRepositoryImpl
import com.example.nexcart.data.repository.StorageRepositoryImpl
import com.example.nexcart.domain.repository.ProductRepository
import com.example.nexcart.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindProductRepository(productRepositoryImpl: ProductRepositoryImpl): ProductRepository

    @Binds
    abstract fun bindStorageRepository(storageRepositoryImpl: StorageRepositoryImpl): StorageRepository
}