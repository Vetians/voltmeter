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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.ukrida.voltmeter.ui.navigation.BottomNav
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    role: String,
    navController: NavHostController,
    viewModel: VoltMeterViewModel
) {
    val innerNavController = rememberNavController()
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
                    Text("VoltMeter")
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
            BottomNav(innerNavController)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedCustomer == null) {
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
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
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
        }
    }
}
