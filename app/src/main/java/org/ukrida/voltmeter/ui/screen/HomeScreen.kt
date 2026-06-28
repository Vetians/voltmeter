package org.ukrida.voltmeter.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: VoltMeterViewModel,
    onCustomerClick: (Customer) -> Unit = {}
) {
    val user = viewModel.currentUser.value
    val customers = viewModel.customers.value
    val todayRecords = viewModel.todayRecords.value
    val isLoading = viewModel.isLoading.value
    val lastSync = viewModel.lastSync.value
    val pendingRecords = viewModel.pendingRecords.value
    val verifiedRecords = viewModel.verifiedRecords.value
    val rejectedRecords = viewModel.rejectedRecords.value

    var expandedSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncWorkOrders()
        viewModel.loadTodayRecords()
        viewModel.loadPendingRecords(user?.user_id)
        viewModel.loadVerifiedRecords(user?.user_id)
        viewModel.loadRejectedRecords(user?.user_id)
    }

    // Compute per-meter completion count
    val totalMeters = remember(customers) {
        customers.sumOf { it.meters.size }
    }
    val completedMeters = remember(verifiedRecords) {
        verifiedRecords.size
    }

    val cal = remember { Calendar.getInstance() }
    val currentMonth = cal.get(Calendar.MONTH)
    val currentYear = cal.get(Calendar.YEAR)
    val monthNames = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val monthName = monthNames[currentMonth]

    val deadlines = remember(customers) {
        customers.mapIndexed { index, customer ->
            val day = 20 + (index % 10)
            customer.customer_id to "$day $monthName $currentYear"
        }.toMap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Selamat Datang, ${user?.name ?: "Petugas"}!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Sinkronkan data untuk mulai pencatatan",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bolt,
                value = "$totalMeters",
                label = "Total Meteran",
                color = Color(0xFF1565C0)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = "$completedMeters",
                label = "Selesai",
                color = Color(0xFF4CAF50)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF1565C0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Terakhir Sync",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastSync,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        DropdownSection(
            title = "Daftar Kerja",
            icon = Icons.Default.ListAlt,
            count = customers.size,
            expanded = expandedSection == "kerja",
            onToggle = { expandedSection = if (expandedSection == "kerja") null else "kerja" }
        ) {
            if (customers.isEmpty()) {
                Text(
                    text = "Tidak ada data pekerjaan",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                customers.forEach { customer ->
                    CustomerWorkCard(
                        name = customer.name,
                        address = customer.address,
                        deadline = deadlines[customer.customer_id] ?: "-",
                        onClick = {
                            viewModel.selectCustomer(customer)
                            onCustomerClick(customer)
                        }
                    )
                }
            }
        }

        DropdownSection(
            title = "Pending",
            icon = Icons.Default.Schedule,
            count = pendingRecords.size,
            expanded = expandedSection == "pending",
            onToggle = { expandedSection = if (expandedSection == "pending") null else "pending" }
        ) {
            if (pendingRecords.isEmpty()) {
                Text(
                    text = "Tidak ada pekerjaan pending",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                pendingRecords.forEach { record ->
                    PendingRecordCard(record = record)
                }
            }
        }

        DropdownSection(
            title = "Selesai",
            icon = Icons.Default.CheckCircle,
            count = verifiedRecords.size,
            expanded = expandedSection == "selesai",
            onToggle = { expandedSection = if (expandedSection == "selesai") null else "selesai" }
        ) {
            if (verifiedRecords.isEmpty()) {
                Text(
                    text = "Belum ada pekerjaan yang selesai",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                verifiedRecords.forEach { record ->
                    CompletedRecordCard(record = record)
                }
            }
        }

        DropdownSection(
            title = "Tidak Diterima",
            icon = Icons.Default.Cancel,
            count = rejectedRecords.size,
            expanded = expandedSection == "ditolak",
            onToggle = { expandedSection = if (expandedSection == "ditolak") null else "ditolak" }
        ) {
            if (rejectedRecords.isEmpty()) {
                Text(
                    text = "Tidak ada data yang ditolak",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                rejectedRecords.forEach { record ->
                    RejectedRecordCard(
                        record = record,
                        onClick = {
                            val customer = customers.find { it.customer_id == record.customer_id }
                            if (customer != null) {
                                viewModel.selectCustomer(customer)
                                onCustomerClick(customer)
                            }
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tekan tombol sync di pojok kanan bawah untuk mengunduh data penugasan",
                    fontSize = 13.sp,
                    color = Color(0xFF795548)
                )
            }
        }
    }
}

@Composable
private fun DropdownSection(
    title: String,
    icon: ImageVector,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onToggle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1565C0)
                ) {
                    Text(
                        text = "$count",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Tutup" else "Buka",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CustomerWorkCard(
    name: String,
    address: String,
    deadline: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = address,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Deadline: $deadline",
                fontSize = 11.sp,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PendingRecordCard(record: MeterRecord) {
    val name = if (record.customer_name.isNotEmpty()) record.customer_name else record.customer_id
    val address = if (record.customer_address.isNotEmpty()) record.customer_address else "-"
    val statusLabel = when (record.visit_status) {
        "TERBACA_NORMAL" -> "Terbaca Normal"
        "RUMAH_KOSONG" -> "Rumah Kosong"
        "HALANGAN" -> "Halangan"
        else -> record.visit_status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = address,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: $statusLabel",
                fontSize = 12.sp,
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tanggal: ${record.record_date}",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Menunggu verifikasi admin",
                fontSize = 11.sp,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun CompletedRecordCard(record: MeterRecord) {
    val name = if (record.customer_name.isNotEmpty()) record.customer_name else record.customer_id
    val address = if (record.customer_address.isNotEmpty()) record.customer_address else "-"
    val statusLabel = when (record.visit_status) {
        "TERBACA_NORMAL" -> "Terbaca Normal"
        "RUMAH_KOSONG" -> "Rumah Kosong"
        "HALANGAN" -> "Halangan"
        else -> record.visit_status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = address,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$statusLabel - ${record.record_date}",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Terverifikasi",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun RejectedRecordCard(
    record: MeterRecord,
    onClick: () -> Unit
) {
    val name = if (record.customer_name.isNotEmpty()) record.customer_name else record.customer_id
    val address = if (record.customer_address.isNotEmpty()) record.customer_address else "-"
    val statusLabel = when (record.visit_status) {
        "TERBACA_NORMAL" -> "Terbaca Normal"
        "RUMAH_KOSONG" -> "Rumah Kosong"
        "HALANGAN" -> "Halangan"
        else -> record.visit_status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = address,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$statusLabel - ${record.record_date}",
                    fontSize = 11.sp,
                    color = Color(0xFFF44336)
                )
                if (record.verification_note != null && record.verification_note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Alasan: ${record.verification_note}",
                        fontSize = 11.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    totalValue: Int = 0
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (totalValue > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val floatValue = value.toFloatOrNull() ?: 0f
                val progress = if (totalValue > 0) floatValue / totalValue else 0f
                androidx.compose.material3.LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
            }
        }
    }
}
