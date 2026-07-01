package org.ukrida.voltmeter.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(viewModel: VoltMeterViewModel) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Menunggu", "Diterima", "Ditolak")

    val pending = viewModel.pendingRecords.value
    val verified = viewModel.verifiedRecords.value
    val rejected = viewModel.rejectedRecords.value

    LaunchedEffect(Unit) {
        viewModel.loadPendingRecords()
        viewModel.loadVerifiedRecords()
        viewModel.loadRejectedRecords()
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color(0xFF1565C0) else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        val currentList = when (selectedTabIndex) {
            0 -> pending
            1 -> verified
            else -> rejected
        }

        if (viewModel.isLoading.value && currentList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            VerificationList(viewModel, currentList)
        }
    }
}

@Composable
fun VerificationList(viewModel: VoltMeterViewModel, records: List<MeterRecord>) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectNote by remember { mutableStateOf("") }
    var selectedRecordId by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf("") }
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
                            viewModel.verifyRecord(selectedRecordId, "REJECTED", rejectNote, selectedCustomerId)
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

    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak ada data", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(records) { record ->
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
                                Text("ID Pelanggan: ${record.customer_id}", fontSize = 12.sp, color = Color.Gray)
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
                                        selectedCustomerId = record.customer_id
                                        showRejectDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Tolak")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.verifyRecord(record.record_id, "VERIFIED", null, record.customer_id)
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
