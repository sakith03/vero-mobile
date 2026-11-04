package com.example.veromobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.data.connectivity.ConnectivityObserver
import com.example.veromobile.domain.repository.AuthError
import com.example.veromobile.domain.repository.AuthRepository
import com.example.veromobile.domain.repository.AuthResult
import com.example.veromobile.domain.repository.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreen { LOGIN, REGISTER, FORGOT_PASSWORD }

data class AuthUiState(
    val isResolvingSession: Boolean = true,
    val user: AuthUser? = null,
    val screen: AuthScreen = AuthScreen.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val message: String? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val connectivity: ConnectivityObserver
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var observedUser: AuthUser? = null

    init {
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
        viewModelScope.launch {
            repository.authState.collect { user ->
                observedUser = user
                if (!_uiState.value.isSubmitting) {
                    _uiState.update {
                        it.copy(isResolvingSession = false, user = user, password = "", confirmPassword = "")
                    }
                }
            }
        }
    }

    fun onEmailChanged(value: String) = _uiState.update {
        it.copy(email = value, emailError = null, message = null)
    }

    fun onPasswordChanged(value: String) = _uiState.update {
        it.copy(password = value, passwordError = null, message = null)
    }

    fun onConfirmPasswordChanged(value: String) = _uiState.update {
        it.copy(confirmPassword = value, confirmPasswordError = null, message = null)
    }

    fun showLogin() = show(AuthScreen.LOGIN)
    fun showRegister() = show(AuthScreen.REGISTER)
    fun showForgotPassword() = show(AuthScreen.FORGOT_PASSWORD)

    fun signIn() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val emailError = AuthValidator.emailError(state.email)
        val passwordError = if (state.password.isBlank()) "Enter your password" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        submitAuth { repository.signIn(state.email.trim(), state.password) }
    }

    fun register() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val emailError = AuthValidator.emailError(state.email)
        val passwordError = AuthValidator.passwordError(state.password)
        val confirmError = when {
            state.confirmPassword.isBlank() -> "Confirm your password"
            state.password != state.confirmPassword -> "Passwords do not match"
            else -> null
        }
        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, confirmPasswordError = confirmError)
            }
            return
        }
        submitAuth { repository.register(state.email.trim(), state.password) }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val emailError = AuthValidator.emailError(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.sendPasswordReset(state.email.trim())) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        message = "If an account exists for this email, a reset link has been sent."
                    )
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, message = messageFor(result.error, resettingPassword = true))
                }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        observedUser = null
        _uiState.value = AuthUiState(isResolvingSession = false, isOffline = !connectivity.isOnline.value)
    }

    private fun show(screen: AuthScreen) {
        if (_uiState.value.isSubmitting) return
        _uiState.update {
            it.copy(
                screen = screen,
                password = "",
                confirmPassword = "",
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                message = null
            )
        }
    }

    private fun submitAuth(block: suspend () -> AuthResult<AuthUser>) {
        _uiState.update { it.copy(isSubmitting = true, message = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is AuthResult.Success -> {
                    observedUser = result.value
                    _uiState.update {
                        it.copy(
                            isResolvingSession = false,
                            isSubmitting = false,
                            user = result.value,
                            password = "",
                            confirmPassword = "",
                            message = null
                        )
                    }
                }
                is AuthResult.Failure -> {
                    observedUser = null
                    _uiState.update {
                        it.copy(
                        isResolvingSession = false,
                        isSubmitting = false,
                        user = null,
                        password = "",
                        confirmPassword = "",
                        message = messageFor(result.error)
                    )
                    }
                }
            }
        }
    }

    private fun messageFor(error: AuthError, resettingPassword: Boolean = false): String = when (error) {
        AuthError.INVALID_CREDENTIALS -> if (resettingPassword) {
            "Enter a valid email address"
        } else {
            "The email or password is incorrect"
        }
        AuthError.EMAIL_ALREADY_IN_USE -> "An account already exists for this email"
        AuthError.WEAK_PASSWORD -> "Choose a stronger password"
        AuthError.NETWORK -> "Check your internet connection and try again"
        AuthError.TOO_MANY_REQUESTS -> "Too many attempts. Please wait and try again"
        AuthError.PROFILE_SETUP -> "Account setup could not finish. Sign in to try again"
        AuthError.UNKNOWN -> "Something went wrong. Please try again"
    }
}
