package com.example.nexcart.domain.model

sealed class AuthResult {
    data class Success(val data: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}