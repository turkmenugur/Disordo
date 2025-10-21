package com.disordo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disordo.data.repository.AuthRepository
import com.disordo.data.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            userPreferencesRepository.isLoggedInFlow.collect { isLoggedIn ->
                val firebaseUser = authRepository.getCurrentUser()
                if (isLoggedIn && firebaseUser != null) {
                    _authState.value = AuthState.Authenticated(firebaseUser)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = authRepository.signInWithGoogle(idToken)
            if (user != null) {
                userPreferencesRepository.saveLoginState(true, user.uid)
                _authState.value = AuthState.Authenticated(user)
            } else {
                userPreferencesRepository.saveLoginState(false, null)
                _authState.value = AuthState.Error("Google ile giriş başarısız oldu.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            userPreferencesRepository.saveLoginState(false, null)
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun getGoogleSignInIntent(): android.content.Intent {
        return authRepository.getGoogleSignInClient().signInIntent
    }
}
