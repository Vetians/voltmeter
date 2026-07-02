package org.ukrida.voltmeter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.Meter
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterSelectionScreen(
    viewModel: VoltMeterViewModel,
    customer: Customer,
    onMeterSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val savedMeters = viewModel.savedMeters.value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Pilih Meteran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0D47A1)
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Data Pelanggan", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("ID: ${customer.customer_id}", color = Color.Gray, fontSize = 12.sp)
                    Text(customer.address, color = Color.Gray, fontSize = 12.sp)
                    Text("${customer.power_va} VA - ${customer.tariff}", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Meteran tersedia (${customer.meters.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(customer.meters) { index, meter ->
                    val isSaved = index in savedMeters
                    val meterStatus = meter.monthly_status
                    val isMeterBlocked = meterStatus == "VERIFIED" || meterStatus == "PENDING"
                    MeterCard(
                        meter = meter,
                        index = index,
                        isSaved = isSaved,
                        isBlocked = isMeterBlocked,
                        powerVa = customer.power_va,
                        onClick = {
                            viewModel.selectMeter(index)
                            onMeterSelected(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MeterCard(
    meter: Meter,
    index: Int,
    isSaved: Boolean,
    isBlocked: Boolean,
    powerVa: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSaved && !isBlocked) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSaved -> Color(0xFFE8F5E9)
                isBlocked -> Color(0xFFFFF3E0)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            when {
                                isSaved -> listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                isBlocked -> listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
                                else -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSaved) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isBlocked) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meter.meter_number,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stand terakhir: ${meter.last_reading} kWh",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                val maxKwh = (powerVa * 24 * 30) / 1000
                Text(
                    text = "Batas max: $maxKwh kWh/bulan",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            if (isSaved) {
                Text(
                    text = "Selesai",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            } else if (isBlocked) {
                val statusLabel = when (meter.monthly_status) {
                    "VERIFIED" -> "Terverifikasi"
                    "PENDING" -> "Pending"
                    else -> "Terblokir"
                }
                Text(
                    text = statusLabel,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "Input →",
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
