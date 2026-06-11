package com.example.nexcart.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexcart.domain.model.AuthResult
import com.example.nexcart.domain.model.User
import com.example.nexcart.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val forgotPasswordState: StateFlow<AuthUiState> = _forgotPasswordState.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent: SharedFlow<AuthEvent> = _authEvent.asSharedFlow()

    fun signInWithEmail(email: String, password: String) {
        if (!validateEmailPassword(email, password)) return

        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading

            when (val result = authRepository.signInWithEmail(email, password)) {
                is AuthResult.Success -> {
                    _loginState.value = AuthUiState.Success(result.data)
                    _authEvent.emit(AuthEvent.NavigateToMain(result.data.seller))
                }
                is AuthResult.Error -> {
                    _loginState.value = AuthUiState.Error(result.message)
                    _authEvent.emit(AuthEvent.ShowToast(result.message))
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading

            when (val result = authRepository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    _loginState.value = AuthUiState.Success(result.data)
                    _authEvent.emit(AuthEvent.NavigateToMain(result.data.seller))
                }
                is AuthResult.Error -> {
                    _loginState.value = AuthUiState.Error(result.message)
                    _authEvent.emit(AuthEvent.ShowToast(result.message))
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun signUp(name: String, email: String, password: String, isSeller: Boolean) {
        if (!validateEmailPassword(email, password)) return

        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading

            when (val result = authRepository.signUp(name, email, password, isSeller)) {
                is AuthResult.Success -> {
                    _registerState.value = AuthUiState.Success(result.data)
                    _authEvent.emit(AuthEvent.NavigateToMain(result.data.seller))
                }
                is AuthResult.Error -> {
                    _registerState.value = AuthUiState.Error(result.message)
                    _authEvent.emit(AuthEvent.ShowToast(result.message))
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            viewModelScope.launch {
                _authEvent.emit(AuthEvent.ShowToast("Please enter your email address."))
            }
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = AuthUiState.Loading

            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> {
                    _forgotPasswordState.value = AuthUiState.Idle
                    _authEvent.emit(AuthEvent.ShowToast("Reset link sent to $email"))
                }
                is AuthResult.Error -> {
                    _forgotPasswordState.value = AuthUiState.Error(result.message)
                    _authEvent.emit(AuthEvent.ShowToast(result.message))
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        viewModelScope.launch {
            _loginState.value = AuthUiState.Idle
            _authEvent.emit(AuthEvent.NavigateToLogin)
        }
    }

    private fun validateEmailPassword(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                emitError("Email cannot be empty.")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emitError("Please enter a valid email address.")
                false
            }
            password.isBlank() -> {
                emitError("Password cannot be empty.")
                false
            }
            password.length < 6 -> {
                emitError("Password must be at least 6 characters.")
                false
            }
            else -> true
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState.Error(message)
            _authEvent.emit(AuthEvent.ShowToast(message))
        }
    }

    fun resetLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class AuthEvent {
    data class NavigateToMain(val isSeller: Boolean) : AuthEvent()
    object NavigateToLogin : AuthEvent()
    data class ShowToast(val message: String) : AuthEvent()
}