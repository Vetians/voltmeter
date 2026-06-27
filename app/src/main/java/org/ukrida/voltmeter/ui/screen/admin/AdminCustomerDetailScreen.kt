package org.ukrida.voltmeter.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerDetailScreen(
    viewModel: VoltMeterViewModel,
    customerId: String,
    onBack: () -> Unit
) {
    val customer = viewModel.selectedAdminCustomer.value
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Info & Riwayat", "Alat Meter")

    LaunchedEffect(customerId) {
        val found = viewModel.customers.value.find { it.customer_id == customerId }
        viewModel.selectedAdminCustomer.value = found
        viewModel.loadCustomerHistory(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pelanggan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header Info
                CustomerHeaderInfo(customer)

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF1565C0)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, color = if (selectedTabIndex == index) Color(0xFF1565C0) else Color.Gray) }
                        )
                    }
                }

                // Tab Content
                when (selectedTabIndex) {
                    0 -> HistoryTab(viewModel.customerHistory.value)
                    1 -> MetersTab(viewModel, customer)
                }
            }
        }
    }
}

@Composable
fun CustomerHeaderInfo(customer: Customer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = "ID: ${customer.customer_id}", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = customer.address, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("${customer.power_va} VA") })
                AssistChip(onClick = { }, label = { Text(customer.tariff) })
            }
        }
    }
}

@Composable
fun HistoryTab(history: List<MeterRecord>) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada riwayat pencatatan", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(history) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.meter_number, fontWeight = FontWeight.Bold)
                            Text("Tanggal: ${record.record_date} ${record.record_time}", fontSize = 14.sp)
                            Text("Angka: ${record.current_reading}", fontSize = 14.sp, color = Color.DarkGray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Status: ${record.visit_status}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (record.is_verified == 1) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                            else Color(0xFFFF9800).copy(alpha = 0.1f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (record.is_verified == 1) "Selesai" else "Pending",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.is_verified == 1) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetersTab(viewModel: VoltMeterViewModel, customer: Customer) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newMeterNumber by remember { mutableStateOf("") }
    
    val isLoading = viewModel.isLoading.value

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Alat Meter") },
            text = {
                OutlinedTextField(
                    value = newMeterNumber,
                    onValueChange = { newMeterNumber = it },
                    label = { Text("Nomor Meter") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMeterNumber.isNotBlank()) {
                            viewModel.addMeter(customer.customer_id, newMeterNumber)
                            showAddDialog = false
                            newMeterNumber = ""
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah Meteran")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (customer.meters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada alat meter", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customer.meters) { meter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meter.meter_number, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Pemakaian Terakhir: ${meter.last_reading}", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteMeter(meter.meter_number) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
