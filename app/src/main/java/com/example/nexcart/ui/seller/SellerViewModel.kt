package com.example.nexcart.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.repository.AuthRepository
import com.example.nexcart.domain.repository.ProductRepository
import com.example.nexcart.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sellerProducts = MutableStateFlow<UiState>(UiState.Loading)
    val sellerProducts: StateFlow<UiState> = _sellerProducts

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private var allProductsList: List<Product> = emptyList()
    private var currentCategories: List<String> = emptyList()
    private var currentMinPrice: Float = 0f
    private var currentMaxPrice: Float = 1000f
    private var currentQuery: String = ""

    init {
        loadSellerProducts()
    }

    fun loadSellerProducts() {
        viewModelScope.launch {
            _sellerProducts.value = UiState.Loading
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                val products = productRepository.getProductsBySeller(currentUser.uid)
                allProductsList = products
                _categories.value = products.map { it.category }.distinct().sorted()
                performFiltering()
            } else {
                _sellerProducts.value = UiState.Error("User not logged in")
            }
        }
    }

    fun applyFilters(categories: List<String>, minPrice: Float, maxPrice: Float) {
        currentCategories = categories
        currentMinPrice = minPrice
        currentMaxPrice = maxPrice
        performFiltering()
    }

    fun searchProducts(query: String) {
        currentQuery = query
        performFiltering()
    }

    fun resetFilters() {
        currentCategories = emptyList()
        currentMinPrice = 0f
        currentMaxPrice = 1000f
        currentQuery = ""
        performFiltering()
    }

    private fun performFiltering() {
        val filteredList = allProductsList.filter { product ->
            val matchesCategory = if (currentCategories.isEmpty()) {
                true
            } else {
                currentCategories.any { it.equals(product.category, ignoreCase = true) }
            }
            val matchesPrice = product.price >= currentMinPrice && product.price <= currentMaxPrice
            val matchesSearch = if (currentQuery.isEmpty()) {
                true
            } else {
                product.title.contains(currentQuery, ignoreCase = true) ||
                    product.description.contains(currentQuery, ignoreCase = true)
            }
            matchesCategory && matchesPrice && matchesSearch
        }
        _sellerProducts.value = UiState.Success(filteredList)
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                val productWithSeller = product.copy(sellerId = currentUser.uid)
                productRepository.addProduct(productWithSeller).onSuccess {
                    loadSellerProducts()
                }.onFailure {
                    // Handle error
                }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId).onSuccess {
                loadSellerProducts()
            }
        }
    }
}