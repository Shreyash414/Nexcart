package com.example.nexcart.domain.usecase.product

import com.example.nexcart.domain.repository.ProductRepository
import javax.inject.Inject

class GetRecommendedProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke() = productRepository.getRecommendedProducts()
}
