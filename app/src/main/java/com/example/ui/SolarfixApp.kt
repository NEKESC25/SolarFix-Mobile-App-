package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.SystemHubScreen
import com.example.ui.screens.TechnicianScreen
import com.example.ui.screens.TermsOfServiceScreen
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.ui.viewmodel.SystemHubViewModel
import com.example.ui.viewmodel.TechnicianViewModel

object SolarfixRoutes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val VERIFY_EMAIL = "verify_email"
    const val DASHBOARD = "dashboard"
    const val TERMS_OF_SERVICE = "terms_of_service"
    const val PRIVACY_POLICY = "privacy_policy"

    // Backward-compatible alias for existing tests
    const val AUTH = LOGIN
}

enum class SolarfixNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TECHNICIAN("AI Tech", Icons.Default.Bolt),
    MARKETPLACE("Marketplace", Icons.Default.ShoppingCart),
    ANALYTICS("Analytics", Icons.Default.ShowChart),
    SYSTEM_HUB("System Hub", Icons.Default.Build),
    ADMIN("Admin Ops", Icons.Default.AdminPanelSettings)
}

@Composable
fun SolarfixApp(
    authViewModel: AuthViewModel = viewModel(),
    technicianViewModel: TechnicianViewModel = viewModel(),
    marketplaceViewModel: MarketplaceViewModel = viewModel(),
    analyticsViewModel: AnalyticsViewModel = viewModel(),
    systemHubViewModel: SystemHubViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    // Strict Authentication Wall Enforcement
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == SolarfixRoutes.LOGIN || currentRoute == null) {
                navController.navigate(SolarfixRoutes.DASHBOARD) {
                    popUpTo(SolarfixRoutes.LOGIN) { inclusive = true }
                }
            }
        } else {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != SolarfixRoutes.LOGIN &&
                currentRoute != SolarfixRoutes.SIGNUP &&
                currentRoute != SolarfixRoutes.VERIFY_EMAIL &&
                currentRoute != SolarfixRoutes.TERMS_OF_SERVICE &&
                currentRoute != SolarfixRoutes.PRIVACY_POLICY
            ) {
                navController.navigate(SolarfixRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = SolarfixRoutes.LOGIN,
        modifier = Modifier.testTag("solarfix_nav_host")
    ) {
        composable(SolarfixRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(SolarfixRoutes.DASHBOARD) {
                        popUpTo(SolarfixRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(SolarfixRoutes.SIGNUP)
                },
                onNavigateToForgotPassword = {
                    authViewModel.sendPasswordReset()
                }
            )
        }

        composable(SolarfixRoutes.SIGNUP) {
            SignUpScreen(
                viewModel = authViewModel,
                onCodeSent = {
                    navController.navigate(SolarfixRoutes.VERIFY_EMAIL)
                },
                onNavigateToLogin = {
                    navController.navigate(SolarfixRoutes.LOGIN) {
                        popUpTo(SolarfixRoutes.SIGNUP) { inclusive = true }
                    }
                }
            )
        }

        composable(SolarfixRoutes.VERIFY_EMAIL) {
            EmailVerificationScreen(
                viewModel = authViewModel,
                onVerificationSuccess = {
                    navController.navigate(SolarfixRoutes.DASHBOARD) {
                        popUpTo(SolarfixRoutes.LOGIN) { inclusive = true }
                    }
                },
                onBackToSignUp = {
                    navController.navigate(SolarfixRoutes.SIGNUP) {
                        popUpTo(SolarfixRoutes.VERIFY_EMAIL) { inclusive = true }
                    }
                }
            )
        }

        composable(SolarfixRoutes.TERMS_OF_SERVICE) {
            TermsOfServiceScreen(
                entryPointLabel = "SolarFix Legal Center",
                userIdentifier = currentUser?.email ?: "visitor",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(SolarfixRoutes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(
                entryPointLabel = "SolarFix Privacy Center",
                userIdentifier = currentUser?.email ?: "visitor",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(SolarfixRoutes.DASHBOARD) {
            val user = currentUser
            if (user != null) {
                MainDashboardScreen(
                    activeUser = user,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(SolarfixRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    technicianViewModel = technicianViewModel,
                    marketplaceViewModel = marketplaceViewModel,
                    analyticsViewModel = analyticsViewModel,
                    systemHubViewModel = systemHubViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(SolarfixRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainDashboardScreen(
    activeUser: UserAccount,
    onLogout: () -> Unit,
    technicianViewModel: TechnicianViewModel,
    marketplaceViewModel: MarketplaceViewModel,
    analyticsViewModel: AnalyticsViewModel,
    systemHubViewModel: SystemHubViewModel
) {
    var selectedTab by remember { mutableStateOf(SolarfixNavTab.HOME) }
    var showFaultManualModal by remember { mutableStateOf(false) }
    val cartCount = marketplaceViewModel.getTotalCartItemsCount()

    // Filter dynamic navigation tabs: Admin Ops shown only if user has isAdmin privilege
    val navTabs = remember(activeUser.isAdmin) {
        if (activeUser.isAdmin) {
            SolarfixNavTab.values().toList()
        } else {
            SolarfixNavTab.values().filter { it != SolarfixNavTab.ADMIN }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("solarfix_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                navTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == SolarfixNavTab.MARKETPLACE && cartCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("$cartCount") }
                                    }
                                ) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                SolarfixNavTab.HOME -> {
                    HomeScreen(
                        user = activeUser,
                        onNavigateToTechnician = { selectedTab = SolarfixNavTab.TECHNICIAN },
                        onNavigateToMarketplace = { selectedTab = SolarfixNavTab.MARKETPLACE },
                        onNavigateToAnalytics = { selectedTab = SolarfixNavTab.ANALYTICS },
                        onNavigateToSystemHub = { selectedTab = SolarfixNavTab.SYSTEM_HUB },
                        onOpenFaultLibrary = { showFaultManualModal = true },
                        onLogoutClick = onLogout,
                        onNavigateToAdmin = if (activeUser.isAdmin) {
                            { selectedTab = SolarfixNavTab.ADMIN }
                        } else null
                    )
                }
                SolarfixNavTab.TECHNICIAN -> {
                    ChatScreen(
                        viewModel = technicianViewModel,
                        onNavigateToSystemHub = { selectedTab = SolarfixNavTab.SYSTEM_HUB },
                        onOpenFaultLibrary = { showFaultManualModal = true }
                    )
                }
                SolarfixNavTab.MARKETPLACE -> {
                    MarketplaceScreen(
                        viewModel = marketplaceViewModel
                    )
                }
                SolarfixNavTab.ANALYTICS -> {
                    AnalyticsScreen(
                        viewModel = analyticsViewModel
                    )
                }
                SolarfixNavTab.SYSTEM_HUB -> {
                    SystemHubScreen(
                        viewModel = systemHubViewModel
                    )
                }
                SolarfixNavTab.ADMIN -> {
                    if (activeUser.isAdmin) {
                        AdminScreen(
                            adminUser = activeUser,
                            onLogoutClick = onLogout,
                            marketplaceViewModel = marketplaceViewModel
                        )
                    } else {
                        selectedTab = SolarfixNavTab.HOME
                    }
                }
            }

            // Inverter Error Codes & Offline Manual Modal
            if (showFaultManualModal) {
                Dialog(
                    onDismissRequest = { showFaultManualModal = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Scaffold(
                        topBar = {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Inverter Fault Library",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { showFaultManualModal = false },
                                        modifier = Modifier.testTag("close_fault_manual_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Manual")
                                    }
                                }
                            }
                        }
                    ) { modalPadding ->
                        Box(modifier = Modifier.padding(modalPadding)) {
                            TechnicianScreen(
                                viewModel = technicianViewModel,
                                onNavigateToSystemHub = {
                                    showFaultManualModal = false
                                    selectedTab = SolarfixNavTab.SYSTEM_HUB
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
