package org.ukrida.voltmeter.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

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
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        val found = viewModel.customers.value.find { it.customer_id == customerId }
        viewModel.selectedAdminCustomer.value = found
        viewModel.loadCustomerHistory(customerId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // tambahkan ini
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Detail Pelanggan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1565C0))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
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
                CustomerHeaderInfo(customer)

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
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

                when (selectedTabIndex) {
                    0 -> HistoryTab(viewModel, customer.customer_id, viewModel.customerHistory.value)
                    1 -> MetersTab(viewModel, customer)
                }
            }
        }
    }

    if (showEditDialog && customer != null) {
        EditCustomerDetailDialog(
            customer = customer,
            onDismiss = { showEditDialog = false },
            onSave = { updatedCustomer ->
                viewModel.updateCustomer(updatedCustomer)
                viewModel.selectedAdminCustomer.value = updatedCustomer
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && customer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus \"${customer.name}\" (${customer.customer_id})? Semua data meteran dan riwayat terkait akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer.customer_id)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
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
fun HistoryTab(viewModel: VoltMeterViewModel, customerId: String, history: List<MeterRecord>) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectNote by remember { mutableStateOf("") }
    var selectedRecordId by remember { mutableStateOf("") }
    var showFullImage by remember { mutableStateOf<String?>(null) }

    if (showFullImage != null) {
        Dialog(onDismissRequest = { showFullImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showFullImage = null },
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = showFullImage,
                    contentDescription = "Full Foto Meteran",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Tolak Laporan") },
            text = {
                OutlinedTextField(
                    value = rejectNote,
                    onValueChange = { rejectNote = it },
                    label = { Text("Alasan Penolakan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectNote.isNotBlank()) {
                            viewModel.verifyRecord(selectedRecordId, "REJECTED", rejectNote, customerId)
                            showRejectDialog = false
                            rejectNote = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tolak")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

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
                val badgeColor = when (record.verification_status) {
                    "VERIFIED" -> Color(0xFF2E7D32)
                    "REJECTED" -> Color(0xFFC62828)
                    else -> Color(0xFFF57C00)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF1565C0))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.meter_number, fontWeight = FontWeight.Bold)
                                Text("Tanggal: ${record.record_date} ${record.record_time}", fontSize = 14.sp)
                                Text("Angka: ${record.current_reading}", fontSize = 14.sp, color = Color.DarkGray)
                                Text("Status Kunjungan: ${record.visit_status}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Surface(
                                color = badgeColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = record.verification_status,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (record.photo_path.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val imageUrl = if (record.photo_path.startsWith("http")) record.photo_path else org.ukrida.voltmeter.data.api.RetrofitInstance.IMAGE_BASE_URL + record.photo_path
                            coil.compose.AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto Meteran",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { showFullImage = imageUrl },
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Detail Lokasi (GPS)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Koordinat GPS: ${record.latitude}, ${record.longitude}", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catatan: Bandingkan koordinat/foto ini dengan Alamat Pelanggan yang terdaftar di sistem untuk verifikasi keaslian.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }

                        if (record.verification_note != null && record.verification_note.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Catatan Admin: ${record.verification_note}", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                        }

                        if (record.verification_status == "PENDING") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedRecordId = record.record_id
                                        showRejectDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Tolak")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.verifyRecord(record.record_id, "VERIFIED", null, customerId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Verifikasi")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerDetailDialog(
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
