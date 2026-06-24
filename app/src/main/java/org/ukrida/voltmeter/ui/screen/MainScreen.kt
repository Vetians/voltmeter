package org.ukrida.voltmeter.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.ukrida.voltmeter.ui.navigation.AdminBottomNav
import org.ukrida.voltmeter.ui.navigation.BottomNav
import org.ukrida.voltmeter.ui.screen.admin.AdminHomeScreen
import org.ukrida.voltmeter.ui.screen.admin.AdminUserScreen
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    role: String,
    navController: NavHostController,
    viewModel: VoltMeterViewModel
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = viewModel.isLoading.value
    val successMessage = viewModel.successMessage.value
    val selectedCustomer = viewModel.selectedCustomer.value

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (role == "admin") "VoltMeter Admin" else "VoltMeter")
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncWorkOrders() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color(0xFF1565C0)
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (role == "admin") {
                AdminBottomNav(innerNavController, currentRoute)
            } else {
                BottomNav(innerNavController)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (role != "admin" && selectedCustomer == null) {
                FloatingActionButton(
                    onClick = { viewModel.syncWorkOrders() },
                    containerColor = Color(0xFF1565C0)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNavController,
            startDestination = if (role == "admin") "admin_dashboard" else "home",
            modifier = Modifier.padding(padding)
        ) {
            // ================== PETUGAS ROUTES ==================
            composable("home") {
                HomeScreen(viewModel = viewModel)
            }

            composable("customer") {
                CustomerListScreen(
                    viewModel = viewModel,
                    onCustomerClick = { customer ->
                        innerNavController.navigate("recording")
                    }
                )
            }

            composable("recording") {
                selectedCustomer?.let { customer ->
                    RecordingScreen(
                        viewModel = viewModel,
                        customer = customer,
                        onRecordingSuccess = {
                            innerNavController.popBackStack()
                        }
                    )
                }
            }

            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }

            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ================== ADMIN ROUTES ==================
            composable("admin_dashboard") {
                AdminHomeScreen(viewModel = viewModel)
            }

            composable("admin_users") {
                AdminUserScreen(
                    viewModel = viewModel,
                    onNavigateToAddUser = {
                        innerNavController.navigate("admin_register_user")
                    }
                )
            }

            composable("admin_register_user") {
                org.ukrida.voltmeter.ui.screen.admin.RegisterScreen(
                    viewModel = viewModel,
                    onBack = {
                        innerNavController.popBackStack()
                    }
                )
            }

            composable("admin_customers") {
                LaunchedEffect(Unit) {
                    viewModel.loadAllCustomers()
                }
                
                // Reuse CustomerListScreen for Admin for now (Read-Only list, clickable to detail)
                CustomerListScreen(
                    viewModel = viewModel,
                    onCustomerClick = { customer ->
                        innerNavController.navigate("admin_customer_detail/${customer.customer_id}")
                    }
                )
            }

            composable("admin_customer_detail/{customerId}") { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                org.ukrida.voltmeter.ui.screen.admin.AdminCustomerDetailScreen(
                    viewModel = viewModel,
                    customerId = customerId,
                    onBack = {
                        innerNavController.popBackStack()
                    }
                )
            }

            composable("admin_profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
