package com.example.nexcart.domain.usecase.favorite

import com.example.nexcart.domain.repository.ProductRepository
import javax.inject.Inject

class GetFavoriteProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke() = productRepository.getFavoriteProducts()
}
