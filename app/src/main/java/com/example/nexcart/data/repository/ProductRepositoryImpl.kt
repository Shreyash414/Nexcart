package com.example.nexcart.data.repository

import android.net.Uri
import com.example.nexcart.data.local.dao.FavoriteDao
import com.example.nexcart.data.local.dao.ProductDao
import com.example.nexcart.data.local.entity.FavoriteProductEntity
import com.example.nexcart.data.local.entity.ProductEntity
import com.example.nexcart.data.remote.ProductApiService
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val favoriteDao: FavoriteDao,
    private val apiService: ProductApiService,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ProductRepository {

    // Cloud/Remote Products via Firestore
    override suspend fun getProducts(): List<Product> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("products").get().await()
            val firestoreProducts = snapshot.toObjects(Product::class.java)
            
            // Cache them in Room
            firestoreProducts.forEach { product ->
                productDao.insertProduct(ProductEntity.fromDomain(product))
            }
            firestoreProducts
        } catch (e: Exception) {
            // Offline fallback to local Room
            try {
                productDao.getAllProducts().map { it.toDomain() }
            } catch (localEx: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getProductsBySeller(sellerId: String): List<Product> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()
            val firestoreProducts = snapshot.toObjects(Product::class.java)
            
            // Cache them in Room
            firestoreProducts.forEach { product ->
                productDao.insertProduct(ProductEntity.fromDomain(product))
            }
            firestoreProducts
        } catch (e: Exception) {
            // Offline fallback to local Room
            try {
                productDao.getProductsBySeller(sellerId).map { it.toDomain() }
            } catch (localEx: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun addProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var uploadUrl = product.image
            
            // If we have a local image, upload it to Firebase Storage
            if (product.image.isNotEmpty() && !product.image.startsWith("http")) {
                try {
                    val file = File(product.image)
                    val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(product.image)
                    val ref = storage.reference.child("products/${product.id}.jpg")
                    uploadImageTask(ref, uri)
                    uploadUrl = ref.downloadUrl.await().toString()
                } catch (storageEx: Exception) {
                    // Fallback to local image path if Firebase Storage fails (e.g. bucket not initialized or restricted)
                    uploadUrl = product.image
                }
            }
            
            val updatedProduct = product.copy(image = uploadUrl)
            
            // Write to Firestore
            firestore.collection("products").document(updatedProduct.id).set(updatedProduct).await()
            
            // Cache in local Room
            val productEntity = ProductEntity.fromDomain(updatedProduct)
            productDao.insertProduct(productEntity)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var uploadUrl = product.image
            
            // If local image is updated, upload it to Firebase Storage
            if (product.image.isNotEmpty() && !product.image.startsWith("http")) {
                try {
                    val file = File(product.image)
                    val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(product.image)
                    val ref = storage.reference.child("products/${product.id}.jpg")
                    uploadImageTask(ref, uri)
                    uploadUrl = ref.downloadUrl.await().toString()
                } catch (storageEx: Exception) {
                    // Fallback to local image path
                    uploadUrl = product.image
                }
            }
            
            val updatedProduct = product.copy(image = uploadUrl)
            
            // Update in Firestore
            firestore.collection("products").document(updatedProduct.id).set(updatedProduct).await()
            
            // Update in Room
            val productEntity = ProductEntity.fromDomain(updatedProduct)
            productDao.updateProduct(productEntity)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete from Firestore
            firestore.collection("products").document(productId).delete().await()
            
            // Delete from Room
            productDao.deleteProduct(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recommended Products (API)
    override suspend fun getRecommendedProducts(): List<Product> = withContext(Dispatchers.IO) {
        try {
            apiService.getProducts()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Favorites (Room)
    override fun getFavoriteProducts(): Flow<List<Product>> {
        return favoriteDao.getAllFavorites().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addFavorite(product: Product) {
        withContext(Dispatchers.IO) {
            favoriteDao.addFavorite(FavoriteProductEntity.fromDomain(product))
        }
    }

    override suspend fun removeFavorite(product: Product) {
        withContext(Dispatchers.IO) {
            favoriteDao.removeFavorite(FavoriteProductEntity.fromDomain(product))
        }
    }

    override suspend fun isFavorite(productId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(productId)
    }

    private suspend fun uploadImageTask(ref: StorageReference, uri: Uri): UploadTask.TaskSnapshot =
        suspendCancellableCoroutine { continuation ->
            val uploadTask = ref.putFile(uri)
            uploadTask.addOnSuccessListener { snapshot ->
                if (continuation.isActive) continuation.resume(snapshot)
            }.addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
            continuation.invokeOnCancellation {
                uploadTask.cancel()
            }
        }
}
