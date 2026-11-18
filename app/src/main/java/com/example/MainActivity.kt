package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AuthManager
import com.example.data.AuthManager.authStateFlow
import com.example.data.CafeRepository
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminOrdersScreen
import com.example.ui.admin.AdminProductsScreen
import com.example.ui.admin.AdminSalesScreen
import com.example.ui.admin.AdminSettingsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.customer.AboutUsScreen
import com.example.ui.customer.CartScreen
import com.example.ui.customer.CheckoutScreen
import com.example.ui.customer.HomeScreen
import com.example.ui.customer.MenuScreen
import com.example.ui.customer.OrderConfirmationScreen
import com.example.ui.customer.OrderTrackingScreen
import com.example.ui.customer.OrdersScreen
import com.example.ui.customer.ProductCustomizationScreen
import com.example.ui.customer.ProfileScreen
import com.example.ui.theme.CafeAntonioLunaTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CafeAntonioLunaTheme {
                AppNavigation()
            }
        }
    }
}

private object Routes {
    const val CUSTOMER_HOME = "customer_home"
    const val CUSTOMER_MENU = "customer_menu"
    const val CUSTOMER_ORDERS = "customer_orders"
    const val CUSTOMER_CART = "customer_cart"
    const val CUSTOMER_PROFILE = "customer_profile"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val CHECKOUT = "checkout"
    const val ORDER_CONFIRMATION = "order_confirmation/{orderId}"
    const val ORDER_TRACKING = "order_tracking/{orderId}"
    const val ABOUT_US = "about_us"

    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_PRODUCTS = "admin_products"
    const val ADMIN_ORDERS = "admin_orders"
    const val ADMIN_SALES = "admin_sales"
    const val ADMIN_SETTINGS = "admin_settings"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val customerTabs = listOf(
    NavTabItem(Routes.CUSTOMER_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavTabItem(Routes.CUSTOMER_MENU, "Menu", Icons.Filled.Coffee, Icons.Outlined.Coffee, "nav_menu"),
    NavTabItem(Routes.CUSTOMER_ORDERS, "Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_orders"),
    NavTabItem(Routes.CUSTOMER_CART, "Cart", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, "nav_cart"),
    NavTabItem(Routes.CUSTOMER_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

private val adminTabs = listOf(
    NavTabItem(Routes.ADMIN_DASHBOARD, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "admin_nav_dashboard"),
    NavTabItem(Routes.ADMIN_PRODUCTS, "Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "admin_nav_products"),
    NavTabItem(Routes.ADMIN_ORDERS, "Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "admin_nav_orders"),
    NavTabItem(Routes.ADMIN_SALES, "Sales", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp, "admin_nav_sales"),
    NavTabItem(Routes.ADMIN_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "admin_nav_settings")
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ })
    } else {
        val isCloudAdmin = AuthManager.isBootstrappedAdmin(user)
        key(user.uid) {
            AuthenticatedCafeApp(currentUserId = user.uid, isCloudAdmin = isCloudAdmin)
        }
    }
}

@Composable
private fun AuthenticatedCafeApp(
    currentUserId: String,
    isCloudAdmin: Boolean
) {
    val customerViewModel: CustomerViewModel = viewModel(
        key = "customer_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CustomerViewModel(CafeRepository(db), currentUserId, isCloudAdmin)
            }
        }
    )

    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AdminViewModel(CafeRepository(db), isCloudAdmin)
            }
        }
    )

    var isAdminMode by rememberSaveable { mutableStateOf(false) }
    val customerNavController = rememberNavController()
    val adminNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val customerFeedback by customerViewModel.feedbackMessage.collectAsStateWithLifecycle()
    val adminFeedback by adminViewModel.feedbackMessage.collectAsStateWithLifecycle()
    val cartState by customerViewModel.cartItems.collectAsStateWithLifecycle()
    val cartCount = ((cartState as? UiState.Success)?.data ?: emptyList()).sumOf { it.quantity }.toInt()

    LaunchedEffect(customerFeedback) {
        customerFeedback?.let {
            snackbarHostState.showSnackbar(it)
            customerViewModel.clearFeedback()
        }
    }
    LaunchedEffect(adminFeedback) {
        adminFeedback?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearFeedback()
        }
    }

    if (isAdminMode) {
        BackHandler { isAdminMode = false }
    }

    val activeController = if (isAdminMode) adminNavController else customerNavController
    val navBackStack by activeController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showTopLevelNav = if (isAdminMode) {
        currentRoute in adminTabs.map { it.route }
    } else {
        currentRoute in customerTabs.map { it.route }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val tabs = if (isAdminMode) adminTabs else customerTabs

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    activeController.navigate(tab.route) {
                                        popUpTo(activeController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (!isAdminMode && tab.route == Routes.CUSTOMER_CART && cartCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    activeController.navigate(tab.route) {
                                        popUpTo(activeController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (!isAdminMode && tab.route == Routes.CUSTOMER_CART && cartCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (isAdminMode) {
                        NavHost(
                            navController = adminNavController,
                            startDestination = Routes.ADMIN_DASHBOARD
                        ) {
                            composable(Routes.ADMIN_DASHBOARD) {
                                AdminDashboardScreen(
                                    viewModel = adminViewModel,
                                    onNavigateToOrders = { adminNavController.navigate(Routes.ADMIN_ORDERS) },
                                    onNavigateToProducts = { adminNavController.navigate(Routes.ADMIN_PRODUCTS) }
                                )
                            }
                            composable(Routes.ADMIN_PRODUCTS) {
                                AdminProductsScreen(viewModel = adminViewModel)
                            }
                            composable(Routes.ADMIN_ORDERS) {
                                AdminOrdersScreen(viewModel = adminViewModel)
                            }
                            composable(Routes.ADMIN_SALES) {
                                AdminSalesScreen(viewModel = adminViewModel)
                            }
                            composable(Routes.ADMIN_SETTINGS) {
                                AdminSettingsScreen(
                                    viewModel = adminViewModel,
                                    onSwitchToCustomer = { isAdminMode = false }
                                )
                            }
                        }
                    } else {
                        NavHost(
                            navController = customerNavController,
                            startDestination = Routes.CUSTOMER_HOME
                        ) {
                            composable(Routes.CUSTOMER_HOME) {
                                HomeScreen(
                                    viewModel = customerViewModel,
                                    onOpenMenu = {
                                        customerNavController.navigate(Routes.CUSTOMER_MENU) {
                                            popUpTo(customerNavController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenProduct = { id -> customerNavController.navigate("product_detail/$id") },
                                    onOpenAboutUs = { customerNavController.navigate(Routes.ABOUT_US) }
                                )
                            }
                            composable(Routes.CUSTOMER_MENU) {
                                MenuScreen(
                                    viewModel = customerViewModel,
                                    onOpenProduct = { id -> customerNavController.navigate("product_detail/$id") }
                                )
                            }
                            composable(Routes.CUSTOMER_ORDERS) {
                                OrdersScreen(
                                    viewModel = customerViewModel,
                                    onOpenOrderTracking = { id -> customerNavController.navigate("order_tracking/$id") }
                                )
                            }
                            composable(Routes.CUSTOMER_CART) {
                                CartScreen(
                                    viewModel = customerViewModel,
                                    onBrowseMenu = { customerNavController.navigate(Routes.CUSTOMER_MENU) },
                                    onProceedToCheckout = { customerNavController.navigate(Routes.CHECKOUT) }
                                )
                            }
                            composable(Routes.CUSTOMER_PROFILE) {
                                ProfileScreen(
                                    viewModel = customerViewModel,
                                    isCloudAdmin = isCloudAdmin,
                                    onOpenAboutUs = { customerNavController.navigate(Routes.ABOUT_US) },
                                    onSwitchToAdmin = { isAdminMode = true }
                                )
                            }
                            composable(
                                route = Routes.PRODUCT_DETAIL,
                                arguments = listOf(navArgument("productId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                                ProductCustomizationScreen(
                                    productId = productId,
                                    viewModel = customerViewModel,
                                    onNavigateBack = { customerNavController.popBackStack() },
                                    onAddedToCart = { customerNavController.popBackStack() }
                                )
                            }
                            composable(Routes.CHECKOUT) {
                                CheckoutScreen(
                                    viewModel = customerViewModel,
                                    onNavigateBack = { customerNavController.popBackStack() },
                                    onOrderPlaced = { orderId ->
                                        customerNavController.navigate("order_confirmation/$orderId") {
                                            popUpTo(Routes.CUSTOMER_CART) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(
                                route = Routes.ORDER_CONFIRMATION,
                                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                                OrderConfirmationScreen(
                                    orderId = orderId,
                                    viewModel = customerViewModel,
                                    onTrackOrder = { id ->
                                        customerNavController.navigate("order_tracking/$id") {
                                            popUpTo(Routes.CUSTOMER_HOME)
                                        }
                                    },
                                    onBackHome = {
                                        customerNavController.navigate(Routes.CUSTOMER_HOME) {
                                            popUpTo(Routes.CUSTOMER_HOME) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(
                                route = Routes.ORDER_TRACKING,
                                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                                OrderTrackingScreen(
                                    orderId = orderId,
                                    viewModel = customerViewModel,
                                    onNavigateBack = { customerNavController.popBackStack() }
                                )
                            }
                            composable(Routes.ABOUT_US) {
                                AboutUsScreen(
                                    onNavigateBack = { customerNavController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
