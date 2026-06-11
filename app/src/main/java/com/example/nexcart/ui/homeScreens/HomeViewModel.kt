package com.example.nexcart.ui.homeScreens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexcart.domain.model.Product
import com.example.nexcart.domain.usecase.product.GetProductsUseCase
import com.example.nexcart.domain.usecase.product.GetRecommendedProductsUseCase
import com.example.nexcart.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getRecommendedProductsUseCase: GetRecommendedProductsUseCase
) : ViewModel() {

    private val _products = MutableStateFlow<UiState>(UiState.Loading)
    val products: StateFlow<UiState> = _products.asStateFlow()

    private val _recommendedProducts = MutableStateFlow<UiState>(UiState.Loading)
    val recommendedProducts: StateFlow<UiState> = _recommendedProducts.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private var allProductsList: List<Product> = emptyList()
    private var currentCategories: List<String> = emptyList()
    private var currentMinPrice: Float = 0f
    private var currentMaxPrice: Float = 1000f
    private var currentQuery: String = ""

    init {
        refresh()
    }

    fun refresh() {
        getProducts()
        getRecommendedProducts()
    }

    private fun getProducts() {
        viewModelScope.launch {
            try {
                _products.value = UiState.Loading
                val products = getProductsUseCase()
                allProductsList = products
                _categories.value = products.map { it.category }.distinct().sorted()
                performFiltering()
            } catch (e: Exception) {
                _products.value = UiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    private fun getRecommendedProducts() {
        viewModelScope.launch {
            try {
                _recommendedProducts.value = UiState.Loading
                val products = getRecommendedProductsUseCase()
                _recommendedProducts.value = UiState.Success(products)
            } catch (e: Exception) {
                _recommendedProducts.value = UiState.Error(e.localizedMessage ?: "An error occurred")
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
        _products.value = UiState.Success(filteredList)
    }
}
