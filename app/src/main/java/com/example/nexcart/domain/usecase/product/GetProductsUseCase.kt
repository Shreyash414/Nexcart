package com.example.nexcart.domain.usecase.product

import com.example.nexcart.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke() = productRepository.getProducts()
}