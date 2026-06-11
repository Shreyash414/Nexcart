package com.example.nexcart.ui.seller

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.repository.AuthRepository
import com.example.nexcart.domain.repository.ProductRepository
import com.example.nexcart.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UiState>(UiState.Idle)
    val uploadState: StateFlow<UiState> = _uploadState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProductEvent>()
    val eventFlow: SharedFlow<AddProductEvent> = _eventFlow.asSharedFlow()

    fun uploadProduct(
        title: String,
        price: String,
        category: String,
        description: String,
        imageUri: Uri?
    ) {
        // Only Title and Price are strictly mandatory for a professional listing
        if (title.isBlank() || price.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(AddProductEvent.ShowToast("Please enter at least a name and price"))
            }
            return
        }

        val priceDouble = price.toDoubleOrNull()
        if (priceDouble == null) {
            viewModelScope.launch {
                _eventFlow.emit(AddProductEvent.ShowToast("Invalid price format"))
            }
            return
        }

        viewModelScope.launch {
            _uploadState.value = UiState.Loading
            
            try {
                val currentUser = authRepository.getCurrentUser()
                val sellerId = currentUser?.uid ?: "local_user"

                // 1. Handle optional image
                val localImageUrl = if (imageUri != null) {
                    saveImageToInternalStorage(imageUri) ?: ""
                } else {
                    ""
                }

                // 2. Save Product to Room
                val product = Product(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    price = priceDouble,
                    category = category,
                    description = description,
                    image = localImageUrl,
                    sellerId = sellerId
                )
                
                productRepository.addProduct(product).onSuccess {
                    _uploadState.value = UiState.Success(Unit)
                    _eventFlow.emit(AddProductEvent.ProductAdded)
                }.onFailure { e ->
                    _uploadState.value = UiState.Error(e.message ?: "Failed to save to database")
                }
            } catch (e: Exception) {
                _uploadState.value = UiState.Error(e.localizedMessage ?: "An unexpected error occurred")
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val fileName = "prod_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    sealed class AddProductEvent {
        data class ShowToast(val message: String) : AddProductEvent()
        object ProductAdded : AddProductEvent()
    }
}
