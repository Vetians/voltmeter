package org.ukrida.voltmeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.ukrida.voltmeter.di.Injection
import org.ukrida.voltmeter.ui.screen.LoginScreen
import org.ukrida.voltmeter.ui.screen.MainScreen
import org.ukrida.voltmeter.ui.theme.VoltMeterTheme
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val voltMeterViewModel = remember { VoltMeterViewModel(Injection.voltMeterRepo) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var role by remember { mutableStateOf("") }

            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) "main" else "login"
            ) {
                // ================= LOGIN =================
                composable("login") {
                    LoginScreen(
                        viewModel = voltMeterViewModel,
                        onLoginSuccess = { userRole ->
                            role = userRole
                            isLoggedIn = true
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                // ================= MAIN =================
                composable("main") {
                    MainScreen(
                        role = role,
                        navController = navController,
                        viewModel = voltMeterViewModel
                    )
                }
            }
        }
    }
}
