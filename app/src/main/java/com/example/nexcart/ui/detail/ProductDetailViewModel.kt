package com.example.nexcart.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.repository.AuthRepository
import com.example.nexcart.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _sellerName = MutableStateFlow("")
    val sellerName: StateFlow<String> = _sellerName.asStateFlow()

    fun checkFavoriteStatus(productId: String) {
        viewModelScope.launch {
            _isFavorite.value = productRepository.isFavorite(productId)
        }
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            if (_isFavorite.value) {
                productRepository.removeFavorite(product)
            } else {
                productRepository.addFavorite(product)
            }
            _isFavorite.value = !_isFavorite.value
        }
    }

    fun loadSellerInfo(sellerId: String) {
        // In a real app, you'd fetch this from Firestore 'users' collection
        // For now, setting a placeholder or fetching if needed
        viewModelScope.launch {
            // Simplified: could use authRepository.getUserById(sellerId) if implemented
            _sellerName.value = "NexCart Seller" 
        }
    }
}
