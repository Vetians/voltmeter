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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(viewModel: VoltMeterViewModel) {
    val stats = viewModel.adminStats.value
    val user = viewModel.currentUser.value
    val isLoading = viewModel.isLoading.value

    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dateFormat.format(Date(currentTime)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1565C0)
                )
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1)
                )
            }
        }

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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
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

                IconButton(
                    onClick = {
                        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                        val currentYearVal = Calendar.getInstance().get(Calendar.YEAR)
                        viewModel.selectedAdminMonth.value = currentMonth
                        viewModel.selectedAdminYear.value = currentYearVal
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset ke Bulan Ini", tint = Color(0xFF1565C0))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        StatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.People,
            value = "${stats.total_pelanggan}",
            label = "Total Pelanggan",
            color = Color(0xFF7B1FA2)
        )

        StatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Bolt,
            value = "${stats.total_meteran}",
            label = "Total Meteran",
            color = Color(0xFF0288D1)
        )

        StatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.AutoMirrored.Filled.List,
            value = "${stats.total_kunjungan}",
            label = "Total Kunjungan",
            color = Color(0xFF1565C0)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = "${stats.total_verified}",
                label = "Terverifikasi",
                color = Color(0xFF4CAF50)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                value = "${stats.total_unverified}",
                label = "Belum Diverifikasi",
                color = Color(0xFFFF9800)
            )
        }
    }
}
