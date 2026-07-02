package org.ukrida.voltmeter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.Meter
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@Composable
fun CustomerListScreen(
    viewModel: VoltMeterViewModel,
    onCustomerClick: (Customer) -> Unit
) {
    val customers = viewModel.customers.value
    val user = viewModel.currentUser.value
    val isAdmin = user?.role == "admin"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingCustomer by remember { mutableStateOf<Customer?>(null) }
    var blockedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedFilter by remember { mutableStateOf("semua") }

    val filteredCustomers = remember(customers, selectedFilter) {
        when (selectedFilter) {
            "verified" -> customers.filter { it.monthly_status == "VERIFIED" }
            "pending" -> customers.filter { it.monthly_status == "PENDING" }
            "rejected" -> customers.filter { it.monthly_status == "REJECTED" }
            else -> customers
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // tambahkan ini
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Pelanggan")
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        if (customers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada data pelanggan",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tekan tombol sync untuk mengunduh data",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 8.dp),
            ) {
                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            "semua" to "Semua",
                            "verified" to "Terverifikasi",
                            "pending" to "Menunggu",
                            "rejected" to "Ditolak"
                        )
                        filters.forEach { (key, label) ->
                            val isSelected = selectedFilter == key
                            val bgColor = when (key) {
                                "verified" -> Color(0xFF4CAF50)
                                "pending" -> Color(0xFFFF9800)
                                "rejected" -> Color(0xFFF44336)
                                else -> Color(0xFF1565C0)
                            }
                            val count = when (key) {
                                "verified" -> customers.count { it.monthly_status == "VERIFIED" }
                                "pending" -> customers.count { it.monthly_status == "PENDING" }
                                "rejected" -> customers.count { it.monthly_status == "REJECTED" }
                                else -> customers.size
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = key },
                                label = {
                                    Text(
                                        text = "$label ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = bgColor.copy(alpha = 0.2f),
                                    selectedLabelColor = bgColor
                                )
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 80.dp
                    )
                ) {
                    items(filteredCustomers) { customer ->
                    CustomerCard(
                        customer = customer,
                        onClick = {
                            if (isAdmin) {
                                viewModel.selectCustomer(customer)
                                onCustomerClick(customer)
                            } else {
                                val canRecord = viewModel.canRecord(customer)
                                if (canRecord) {
                                    viewModel.selectCustomer(customer)
                                    onCustomerClick(customer)
                                } else {
                                    blockedCustomer = customer
                                }
                            }
                        },
                        onEdit = { editingCustomer = customer },
                        onDelete = { deletingCustomer = customer; showDeleteDialog = true },
                        isAdmin = isAdmin
                    )
                }
            }
        }
    }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newCustomer ->
                viewModel.addCustomer(newCustomer)
                showAddDialog = false
            }
        )
    }

    editingCustomer?.let { customer ->
        EditCustomerDialog(
            customer = customer,
            onDismiss = { editingCustomer = null },
            onSave = { updatedCustomer ->
                viewModel.updateCustomer(updatedCustomer)
                editingCustomer = null
            }
        )
    }

    if (showDeleteDialog && deletingCustomer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; deletingCustomer = null },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus \"${deletingCustomer!!.name}\" (${deletingCustomer!!.customer_id})? Semua data meteran dan riwayat terkait akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(deletingCustomer!!.customer_id)
                        showDeleteDialog = false
                        deletingCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; deletingCustomer = null }) {
                    Text("Batal")
                }
            }
        )
    }

    blockedCustomer?.let { customer ->
        val reason = viewModel.getRecordBlockReason(customer)
        AlertDialog(
            onDismissRequest = { blockedCustomer = null },
            title = { Text("Tidak Bisa Input") },
            text = { Text(reason ?: "Pencatatan tidak tersedia untuk pelanggan ini.") },
            confirmButton = {
                TextButton(onClick = { blockedCustomer = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var address by remember { mutableStateOf(customer.address) }
    var powerVa by remember { mutableStateOf(customer.power_va.toString()) }
    var tariff by remember { mutableStateOf(customer.tariff) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Pelanggan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )

                OutlinedTextField(
                    value = customer.customer_id,
                    onValueChange = {},
                    label = { Text("ID Pelanggan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    enabled = false
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = powerVa,
                        onValueChange = { powerVa = it },
                        label = { Text("Daya (VA)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = tariff,
                        onValueChange = { tariff = it },
                        label = { Text("Tarif (R1/R2)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val updatedCustomer = customer.copy(
                                name = name,
                                address = address,
                                power_va = powerVa.toIntOrNull() ?: customer.power_va,
                                tariff = tariff
                            )
                            onSave(updatedCustomer)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = name.isNotBlank() && address.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var customerId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var powerVa by remember { mutableStateOf("") }
    var tariff by remember { mutableStateOf("") }
    var meterNumber by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tambah Pelanggan Baru",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )

                OutlinedTextField(
                    value = customerId,
                    onValueChange = { customerId = it },
                    label = { Text("ID Pelanggan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = powerVa,
                        onValueChange = { powerVa = it },
                        label = { Text("Daya (VA)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = tariff,
                        onValueChange = { tariff = it },
                        label = { Text("Tarif (R1/R2)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = { meterNumber = it },
                    label = { Text("Nomor Meteran Utama") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val newCustomer = Customer(
                                customer_id = customerId,
                                name = name,
                                address = address,
                                power_va = powerVa.toIntOrNull() ?: 0,
                                tariff = tariff,
                                meters = listOf(Meter(meter_number = meterNumber, last_reading = 0.0))
                            )
                            onSave(newCustomer)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = customerId.isNotBlank() && name.isNotBlank() && meterNumber.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    isAdmin: Boolean = false
) {
    val statusColor = when (customer.monthly_status) {
        "VERIFIED" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFF9800)
        "REJECTED" -> Color(0xFFF44336)
        else -> null
    }
    val statusLabel = when (customer.monthly_status) {
        "VERIFIED" -> "Terverifikasi"
        "PENDING" -> "Menunggu Verifikasi"
        "REJECTED" -> "Ditolak"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ID: ${customer.customer_id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${customer.power_va} VA",
                            fontSize = 10.sp,
                            color = Color(0xFF1565C0)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = customer.tariff,
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (customer.meters.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${customer.meters.size} Meter",
                                fontSize = 10.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
                if (statusLabel != null && statusColor != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isAdmin) {
                Column {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1565C0))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                    }
                }
            }
        }
    }
}
