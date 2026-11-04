package com.example.ui.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserAccount
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    LOGIN,
    SIGN_UP,
    FORGOT_PASSWORD
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(application)
) : AndroidViewModel(application) {

    private val _screenMode = MutableStateFlow(AuthScreenMode.LOGIN)
    val screenMode: StateFlow<AuthScreenMode> = _screenMode.asStateFlow()

    val currentUser: StateFlow<UserAccount?> = authRepository.currentUser

    // Form inputs
    val emailInput = MutableStateFlow("")
    val passwordInput = MutableStateFlow("")
    val nameInput = MutableStateFlow("")
    val confirmPasswordInput = MutableStateFlow("")
    val verificationCodeInput = MutableStateFlow("")

    val isPasswordVisible = MutableStateFlow(false)
    val isConfirmPasswordVisible = MutableStateFlow(false)

    val pendingVerification = authRepository.pendingVerification
    val lastDispatchedCode = MutableStateFlow<String?>(null)

    // UI state feedback
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun setScreenMode(mode: AuthScreenMode) {
        _screenMode.value = mode
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun togglePasswordVisibility() {
        isPasswordVisible.value = !isPasswordVisible.value
    }

    fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible.value = !isConfirmPasswordVisible.value
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    fun login(onSuccess: () -> Unit = {}) {
        val email = emailInput.value.trim()
        val password = passwordInput.value

        if (!validateEmail(email)) {
            _errorMessage.value = "Please enter a valid email address (e.g., name@domain.com)"
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Password cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.login(email, password)
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "Welcome back, ${it.name}!"
                onSuccess()
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Authentication failed. Please verify credentials."
            }
        }
    }

    /**
     * Initiates sign up by validating inputs and dispatching a 6-digit email verification code.
     */
    fun initiateSignUp(onCodeSent: () -> Unit = {}) {
        val name = nameInput.value.trim()
        val email = emailInput.value.trim()
        val password = passwordInput.value
        val confirmPassword = confirmPasswordInput.value

        if (name.length < 2) {
            _errorMessage.value = "Please enter your full name (minimum 2 characters)"
            return
        }
        if (!validateEmail(email)) {
            _errorMessage.value = "Please enter a valid email address"
            return
        }
        val pwdError = com.example.util.SecuritySanitizer.validatePasswordStrength(password)
        if (pwdError != null) {
            _errorMessage.value = pwdError
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Passwords do not match. Please re-enter."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val result = authRepository.initiateSignUpWithVerification(name, email, password)
        _isLoading.value = false
        result.onSuccess { code ->
            lastDispatchedCode.value = code
            verificationCodeInput.value = ""
            _successMessage.value = "A 6-digit verification code has been dispatched to $email"
            onCodeSent()
        }.onFailure { ex ->
            _errorMessage.value = ex.message ?: "Could not initiate registration. Try again."
        }
    }

    /**
     * Verifies the 6-digit code entered by the user and finalizes account creation.
     */
    fun verifyEmailCode(onSuccess: () -> Unit = {}) {
        val code = verificationCodeInput.value.trim()
        if (code.length != 6) {
            _errorMessage.value = "Please enter the complete 6-digit verification code."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.verifyEmailCode(code)
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "Email verified! Welcome to Solarfix, ${it.name}!"
                onSuccess()
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Verification failed. Invalid or expired code."
            }
        }
    }

    fun resendVerificationCode() {
        _errorMessage.value = null
        val result = authRepository.resendVerificationCode()
        result.onSuccess { newCode ->
            lastDispatchedCode.value = newCode
            _successMessage.value = "A fresh 6-digit verification code has been sent to your email!"
        }.onFailure { ex ->
            _errorMessage.value = ex.message ?: "Could not resend code. Please try signing up again."
        }
    }

    fun cancelVerification(onCancelled: () -> Unit) {
        authRepository.clearPendingVerification()
        verificationCodeInput.value = ""
        onCancelled()
    }

    fun signUp(onSuccess: () -> Unit = {}) {
        initiateSignUp(onCodeSent = onSuccess)
    }

    fun sendPasswordReset() {
        val email = emailInput.value.trim()
        if (!validateEmail(email)) {
            _errorMessage.value = "Please enter a valid email address to receive reset link"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "A password recovery email has been sent to $email. Please check your inbox and spam folder."
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Could not send reset email. Verify email address."
            }
        }
    }

    fun loginAsDemoRole(role: com.example.data.model.UserRole, onSuccess: () -> Unit = {}) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val account = authRepository.loginAsDemoRole(role)
            _isLoading.value = false
            _successMessage.value = "Welcome ${account.role.displayName}: ${account.name}"
            onSuccess()
        }
    }

    fun logout() {
        authRepository.logout()
        passwordInput.value = ""
        confirmPasswordInput.value = ""
        _screenMode.value = AuthScreenMode.LOGIN
    }

    fun recordSafetyConfirmation(accepted: Boolean) {
        authRepository.recordSafetyConfirmation(accepted)
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
