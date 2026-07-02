package org.ukrida.voltmeter.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Checklist
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

sealed class AdminScreen(val route: String, val label: String) {
    object Dashboard : AdminScreen(route = "admin_dashboard", label = "Dashboard")
    object Verification : AdminScreen(route = "admin_verification", label = "Verifikasi")
    object Users : AdminScreen(route = "admin_users", label = "Petugas")
    object Customers : AdminScreen(route = "admin_customers", label = "Pelanggan")
    object Profile : AdminScreen(route = "admin_profile", label = "Profil")
}

@Composable
fun AdminBottomNav(navController: NavHostController, currentRoute: String?) {
    val items = listOf(AdminScreen.Dashboard, AdminScreen.Verification, AdminScreen.Users, AdminScreen.Customers, AdminScreen.Profile)

    NavigationBar(
        containerColor = Color.White
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when (screen) {
                        AdminScreen.Dashboard -> Icon(Icons.Default.Dashboard, contentDescription = screen.label)
                        AdminScreen.Verification -> Icon(Icons.Default.Checklist, contentDescription = screen.label)
                        AdminScreen.Users -> Icon(Icons.Default.Group, contentDescription = screen.label)
                        AdminScreen.Customers -> Icon(Icons.Default.People, contentDescription = screen.label)
                        AdminScreen.Profile -> Icon(Icons.Default.Person, contentDescription = screen.label)
                    }
                },
                label = {
                    Text(
                        screen.label,
                        fontSize = 10.sp,
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
