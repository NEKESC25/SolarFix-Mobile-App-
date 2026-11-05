package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SolarAuthBackdrop
import com.example.ui.components.SolarfixWelcomeLogo
import com.example.ui.viewmodel.AuthViewModel

/**
 * Screen 1B: SignUpScreen
 * Mandatory account creation screen enforcing the authentication wall.
 * Validates Name, Email, Password, and Password Confirmation, then initiates
 * 6-digit email code dispatch for account verification.
 */
@Composable
fun SignUpScreen(
    onCodeSent: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    BackHandler {
        onNavigateToLogin()
    }

    val name by viewModel.nameInput.collectAsStateWithLifecycle()
    val email by viewModel.emailInput.collectAsStateWithLifecycle()
    val password by viewModel.passwordInput.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPasswordInput.collectAsStateWithLifecycle()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsStateWithLifecycle()
    val isConfirmPasswordVisible by viewModel.isConfirmPasswordVisible.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val isMinLength = password.length >= 6
    val isMatching = password.isNotEmpty() && password == confirmPassword

    SolarAuthBackdrop(
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
                    // Header Brand Badge & Eye-Catching Solar Logo
                    SolarfixWelcomeLogo(
                        title = "Solarfix",
                        subtitle = "Africa's Certified Solar Diagnostics & Hardware Marketplace"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Alert / Feedback Banners
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
                                    .testTag("signup_error_banner")
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

                    // Form Container Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFA000)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Join verified solar engineers & system owners",
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
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Email Address Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = { viewModel.emailInput.value = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("name@domain.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Field
                            OutlinedTextField(
                                value = password,
                                onValueChange = { viewModel.passwordInput.value = it },
                                label = { Text("Password (min. 6 characters)") },
                                placeholder = { Text("Create strong password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_password_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirm Password Field
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { viewModel.confirmPasswordInput.value = it },
                                label = { Text("Confirm Password") },
                                placeholder = { Text("Repeat password") },
                                leadingIcon = {
                                    Icon(Icons.Default.LockReset, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                isError = confirmPassword.isNotEmpty() && !isMatching,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.initiateSignUp(onCodeSent = onCodeSent)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_confirm_password_input")
                            )

                            // Real-time Validation Hints
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isMinLength) "✓ 6+ characters" else "• Minimum 6 characters",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMinLength) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (confirmPassword.isNotEmpty()) {
                                    Text(
                                        text = if (isMatching) "✓ Passwords match" else "✗ Passwords do not match",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMatching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Affirmative Legal Consent & Document Review Section (Unselected by default)
                            val termsAccepted by viewModel.termsAcceptedInput.collectAsStateWithLifecycle()
                            val privacyAcknowledged by viewModel.privacyAcknowledgedInput.collectAsStateWithLifecycle()
                            val optionalAiConsent by viewModel.optionalAiConsentInput.collectAsStateWithLifecycle()

                            var showTermsModal by remember { mutableStateOf(false) }
                            var showPrivacyModal by remember { mutableStateOf(false) }
                            if (showTermsModal) {
                                TermsOfServiceModalDialog(
                                    entryPointLabel = "Account Registration",
                                    userIdentifier = email.ifBlank { "new_registration_user" },
                                    onDismiss = { showTermsModal = false },
                                    onAcceptTerms = { viewModel.termsAcceptedInput.value = true }
                                )
                            }
                            if (showPrivacyModal) {
                                PrivacyPolicyModalDialog(
                                    entryPointLabel = "Account Registration",
                                    userIdentifier = email.ifBlank { "new_registration_user" },
                                    onDismiss = { showPrivacyModal = false }
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Legal Document Acceptance & Consent (Required *)",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // 1. Terms of Service Acceptance Checkbox (Unselected by default)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = termsAccepted,
                                            onCheckedChange = { viewModel.termsAcceptedInput.value = it },
                                            modifier = Modifier.testTag("signup_accept_terms_checkbox")
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "I have read and accept the SolarFix Terms of Use & Terms of Service (v3.2) *",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            TextButton(
                                                onClick = { showTermsModal = true },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("signup_terms_of_service_button")
                                            ) {
                                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Open & Read Terms of Service",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // 2. Privacy Policy Acknowledgement Checkbox (Unselected by default)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = privacyAcknowledged,
                                            onCheckedChange = { viewModel.privacyAcknowledgedInput.value = it },
                                            modifier = Modifier.testTag("signup_accept_privacy_checkbox")
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "I have read and acknowledge the SolarFix Privacy Policy & Data Protection Notice (v3.2) *",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            TextButton(
                                                onClick = { showPrivacyModal = true },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("signup_privacy_policy_button")
                                            ) {
                                                Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Open & Read Privacy Policy",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // 3. Optional Consent Checkbox (Unselected by default)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = optionalAiConsent,
                                            onCheckedChange = {
                                                viewModel.optionalAiConsentInput.value = it
                                                viewModel.optionalSafetyAlertsConsentInput.value = it
                                            },
                                            modifier = Modifier.testTag("signup_optional_consent_checkbox")
                                        )
                                        Text(
                                            text = "Optional: Save encrypted local AI Technician diagnostic history & receive high-voltage safety advisories (revocable anytime)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Primary "Register & Send Code" CTA Button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.initiateSignUp(onCodeSent = onCodeSent)
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("register_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Generating Verification Code...")
                                } else {
                                    Icon(Icons.Default.MarkEmailRead, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Register & Send Verification Code",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Secondary "Back to Log In" Button
                            OutlinedButton(
                                onClick = onNavigateToLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("navigate_to_login_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Already have an account? Log In",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Clean Security Reassurance
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
                            text = "Safe & Encrypted Session • Certified Solar Escrow Protection",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
