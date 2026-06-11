package com.example.nexcart.domain.repository

import com.example.nexcart.domain.model.AuthResult
import com.example.nexcart.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithEmail(email: String, pass: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    suspend fun signUp(name: String, email: String, pass: String, isSeller: Boolean): AuthResult
    fun signOut()
    fun getAuthState(): Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun getUserById(uid: String): User?
}
