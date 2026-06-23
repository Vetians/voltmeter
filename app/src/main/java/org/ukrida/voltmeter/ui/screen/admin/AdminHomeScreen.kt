package org.ukrida.voltmeter.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.ui.screen.StatCard
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@Composable
fun AdminHomeScreen(viewModel: VoltMeterViewModel) {
    val stats = viewModel.adminStats.value
    val user = viewModel.currentUser.value

    LaunchedEffect(Unit) {
        viewModel.loadAdminData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dashboard Admin",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Selamat datang, ${user?.name ?: "Admin"}",
            color = Color.Gray,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ListAlt,
                value = "${stats.total_kunjungan}",
                label = "Total Kunjungan",
                color = Color(0xFF1565C0)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = "${stats.terbaca_normal}",
                label = "Terbaca Normal",
                color = Color(0xFF4CAF50)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Home,
                value = "${stats.rumah_kosong}",
                label = "Rumah Kosong",
                color = Color(0xFFFF9800)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                value = "${stats.halangan}",
                label = "Halangan",
                color = Color(0xFFF44336)
            )
        }
    }
}
