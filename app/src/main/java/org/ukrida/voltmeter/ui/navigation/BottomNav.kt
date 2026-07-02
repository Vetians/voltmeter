package org.ukrida.voltmeter.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

sealed class Screen(val route: String, val label: String) {
    object Home : Screen(route = "home", label = "Beranda")
    object Customer : Screen(route = "customer", label = "Pelanggan")
    object History : Screen(route = "history", label = "Riwayat")
    object Profile : Screen(route = "profile", label = "Profil")
}

@Composable
fun BottomNav(navController: NavHostController) {
    val items = listOf(Screen.Home, Screen.Customer, Screen.History, Screen.Profile)

    NavigationBar(
        containerColor = Color.White
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                selected = false,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when (screen) {
                        Screen.Home -> Icon(Icons.Default.Home, contentDescription = screen.label)
                        Screen.Customer -> Icon(Icons.Default.People, contentDescription = screen.label)
                        Screen.History -> Icon(Icons.Default.History, contentDescription = screen.label)
                        Screen.Profile -> Icon(Icons.Default.Person, contentDescription = screen.label)
                    }
                },
                label = {
                    Text(
                        screen.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0D47A1),
                    selectedTextColor = Color(0xFF0D47A1),
                    indicatorColor = Color(0xFFE3F2FD),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
