package com.example.nexcart.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadImage(uri: Uri, path: String): Result<String>
}