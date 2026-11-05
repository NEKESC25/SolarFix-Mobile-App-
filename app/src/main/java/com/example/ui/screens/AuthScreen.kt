package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AuthScreenMode
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenMode by viewModel.screenMode.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    com.example.ui.components.SolarAuthBackdrop(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Brand Badge
                    BrandHeader()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Floating Alert / Feedback Banners
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { error ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .testTag("auth_error_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        successMessage?.let { msg ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .testTag("auth_success_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissSuccess() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content Container Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_container_card"),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(modifier = Modifier.padding(24.dp)) {
                            AnimatedContent(
                                targetState = screenMode,
                                label = "AuthScreenAnimation"
                            ) { mode ->
                                when (mode) {
                                    AuthScreenMode.LOGIN -> {
                                        LoginScreenContent(
                                            viewModel = viewModel,
                                            isLoading = isLoading,
                                            onLoginClick = {
                                                focusManager.clearFocus()
                                                viewModel.login(onSuccess = onAuthSuccess)
                                            },
                                            onNavigateToSignUp = {
                                                viewModel.setScreenMode(AuthScreenMode.SIGN_UP)
                                            },
                                            onNavigateToForgotPassword = {
                                                viewModel.setScreenMode(AuthScreenMode.FORGOT_PASSWORD)
                                            }
                                        )
                                    }
                                    AuthScreenMode.SIGN_UP -> {
                                        SignUpScreenContent(
                                            viewModel = viewModel,
                                            isLoading = isLoading,
                                            onSignUpClick = {
                                                focusManager.clearFocus()
                                                viewModel.signUp(onSuccess = onAuthSuccess)
                                            },
                                            onNavigateToLogin = {
                                                viewModel.setScreenMode(AuthScreenMode.LOGIN)
                                            }
                                        )
                                    }
                                    AuthScreenMode.FORGOT_PASSWORD -> {
                                        ForgotPasswordContent(
                                            viewModel = viewModel,
                                            isLoading = isLoading,
                                            onSendResetLink = {
                                                focusManager.clearFocus()
                                                viewModel.sendPasswordReset()
                                            },
                                            onNavigateToLogin = {
                                                viewModel.setScreenMode(AuthScreenMode.LOGIN)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // African Support & Security Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Secure",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "256-Bit SSL Encrypted • High-Voltage Safety Certified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    com.example.ui.components.SolarfixWelcomeLogo(
        title = "Solarfix",
        subtitle = "Smart Solar Technician & Marketplace"
    )
}

// -------------------------------------------------------------
// Screen 1A: Welcome / Login Screen
// -------------------------------------------------------------
@Composable
private fun LoginScreenContent(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val email by viewModel.emailInput.collectAsStateWithLifecycle()
    val password by viewModel.passwordInput.collectAsStateWithLifecycle()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Sign in to access your solar diagnostics & marketplace",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.emailInput.value = it },
            label = { Text("Email Address") },
            placeholder = { Text("e.g. user@domain.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email Icon")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.passwordInput.value = it },
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password Icon")
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        // Forgot Password Hyperlink
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(
                onClick = onNavigateToForgotPassword,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier.testTag("login_forgot_password_button")
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Primary Log In Button
        Button(
            onClick = onLoginClick,
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  OR  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Secondary "Create New Account" Button
        OutlinedButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("create_account_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create New Account",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        var showLoginTermsModal by remember { mutableStateOf(false) }
        if (showLoginTermsModal) {
            TermsOfServiceModalDialog(
                entryPointLabel = "Login Screen",
                userIdentifier = email.ifBlank { "login_user" },
                onDismiss = { showLoginTermsModal = false }
            )
        }

        // Clean Security & Privacy Reassurance + Terms Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "End-to-End Encrypted • Bank-Grade Solar Escrow",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = { showLoginTermsModal = true },
                modifier = Modifier.testTag("auth_login_terms_button")
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Terms of Use & Terms of Service",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Screen 1B: Sign-Up Form
// -------------------------------------------------------------
@Composable
private fun SignUpScreenContent(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onSignUpClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val name by viewModel.nameInput.collectAsStateWithLifecycle()
    val email by viewModel.emailInput.collectAsStateWithLifecycle()
    val password by viewModel.passwordInput.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPasswordInput.collectAsStateWithLifecycle()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsStateWithLifecycle()
    val isConfirmPasswordVisible by viewModel.isConfirmPasswordVisible.collectAsStateWithLifecycle()

    val isMinLength = password.length >= 6
    val isMatching = password.isNotEmpty() && password == confirmPassword
    var showSignUpTermsModal by remember { mutableStateOf(false) }

    if (showSignUpTermsModal) {
        TermsOfServiceModalDialog(
            entryPointLabel = "Registration Screen",
            userIdentifier = email.ifBlank { "signup_user" },
            onDismiss = { showSignUpTermsModal = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Join thousands of solar owners & technicians",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Full Name Field
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.nameInput.value = it },
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Babatunde Adebayo") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = "User Icon")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.emailInput.value = it },
            label = { Text("Email Address") },
            placeholder = { Text("user@example.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email Icon")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.passwordInput.value = it },
            label = { Text("Password") },
            placeholder = { Text("At least 6 characters") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password Icon")
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { viewModel.confirmPasswordInput.value = it },
            label = { Text("Confirm Password") },
            placeholder = { Text("Re-enter password") },
            leadingIcon = {
                Icon(Icons.Default.LockReset, contentDescription = "Confirm Icon")
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                    Icon(
                        imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle confirm password"
                    )
                }
            },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSignUpClick() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_confirm_password_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Real-Time Password Validation Criteria Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ValidationIndicator(
                label = "6+ chars",
                isValid = isMinLength,
                modifier = Modifier.weight(1f)
            )
            ValidationIndicator(
                label = "Passwords match",
                isValid = isMatching,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val termsAccepted by viewModel.termsAcceptedInput.collectAsStateWithLifecycle()
        val privacyAcknowledged by viewModel.privacyAcknowledgedInput.collectAsStateWithLifecycle()
        var showSignUpPrivacyModal by remember { mutableStateOf(false) }

        if (showSignUpPrivacyModal) {
            PrivacyPolicyModalDialog(
                entryPointLabel = "Registration Screen",
                userIdentifier = email.ifBlank { "signup_user" },
                onDismiss = { showSignUpPrivacyModal = false }
            )
        }

        // Unselected-by-default Affirmative Consent Checkboxes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { viewModel.termsAcceptedInput.value = it },
                modifier = Modifier.testTag("auth_signup_accept_terms_checkbox")
            )
            Text(
                text = "Accept Terms of Service (v3.2) *",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { showSignUpTermsModal = true },
                modifier = Modifier.testTag("auth_signup_terms_button")
            ) {
                Text("Read Terms", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = privacyAcknowledged,
                onCheckedChange = { viewModel.privacyAcknowledgedInput.value = it },
                modifier = Modifier.testTag("auth_signup_accept_privacy_checkbox")
            )
            Text(
                text = "Acknowledge Privacy Policy (v3.2) *",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { showSignUpPrivacyModal = true },
                modifier = Modifier.testTag("auth_signup_privacy_button")
            ) {
                Text("Read Privacy", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Register Button
        Button(
            onClick = onSignUpClick,
            enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && isMinLength && isMatching,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("signup_submit_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Register Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back to Login Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.testTag("back_to_login_button")
            ) {
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Screen 1C: Forgot Password / Recovery Screen
// -------------------------------------------------------------
@Composable
private fun ForgotPasswordContent(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onSendResetLink: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val email by viewModel.emailInput.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Recover Password",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enter your registered email address and we'll send you a secure link to reset your account credentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.emailInput.value = it },
            label = { Text("Registered Email Address") },
            placeholder = { Text("e.g. user@domain.com") },
            leadingIcon = {
                Icon(Icons.Default.MarkEmailUnread, contentDescription = "Email Icon")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSendResetLink() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("forgot_email_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSendResetLink,
            enabled = !isLoading && email.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("forgot_submit_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Reset Link",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("back_to_login_from_reset_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Log In",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun ValidationIndicator(
    label: String,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val icon = if (isValid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isValid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
