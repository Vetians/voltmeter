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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.ui.screen.StatCard
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(viewModel: VoltMeterViewModel) {
    val stats = viewModel.adminStats.value
    val user = viewModel.currentUser.value
    val isLoading = viewModel.isLoading.value

    var expandedMonth by remember { mutableStateOf(false) }
    val months = listOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    val selectedMonthIndex = viewModel.selectedAdminMonth.value ?: 0

    var expandedYear by remember { mutableStateOf(false) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = listOf("Semua Tahun", currentYear.toString(), (currentYear - 1).toString())
    val selectedYearText = viewModel.selectedAdminYear.value?.toString() ?: "Semua Tahun"

    LaunchedEffect(viewModel.selectedAdminMonth.value, viewModel.selectedAdminYear.value) {
        viewModel.loadAdminData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Dashboard Admin", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "Selamat datang, ${user?.name ?: "Admin"}", color = Color.Gray, fontSize = 14.sp)
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
        }

        // Filters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.Gray)
                
                // Month Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = !expandedMonth },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = months[selectedMonthIndex],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false }
                    ) {
                        months.forEachIndexed { index, selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.selectedAdminMonth.value = if (index == 0) null else index
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }

                // Year Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = !expandedYear },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedYearText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        years.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.selectedAdminYear.value = if (selectionOption == "Semua Tahun") null else selectionOption.toInt()
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val totalData = stats.total_kunjungan

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
                // No progress bar for total itself
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = "${stats.terbaca_normal}",
                label = "Terbaca Normal",
                color = Color(0xFF4CAF50),
                totalValue = totalData
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
                color = Color(0xFFFF9800),
                totalValue = totalData
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                value = "${stats.halangan}",
                label = "Halangan",
                color = Color(0xFFF44336),
                totalValue = totalData
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Peringatan Anomali
        Text("Peringatan Terbaru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (stats.rumah_kosong > 0 || stats.halangan > 0) {
                    Text("Terdapat ${stats.rumah_kosong + stats.halangan} kunjungan yang memerlukan perhatian (Rumah Kosong atau Halangan). Silakan tinjau daftar pelanggan untuk detailnya.", color = Color(0xFFE65100), fontSize = 14.sp)
                } else {
                    Text("Tidak ada peringatan khusus saat ini. Semua laporan terbaca normal.", color = Color(0xFF2E7D32), fontSize = 14.sp)
                }
            }
        }
    }
}
