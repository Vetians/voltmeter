package org.ukrida.voltmeter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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

@Composable
fun MeterSelectionScreen(
    viewModel: VoltMeterViewModel,
    customer: Customer,
    onMeterSelected: (Int) -> Unit
) {
    val savedMeters = viewModel.savedMeters.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    // Ambil status per meter dari sumber lokal terbaru. Data customer yang
                    // sedang ada di layar bisa berasal dari sebelum proses sync selesai.
                    val meterStatus = viewModel.getMeterStatus(customer.customer_id, meter.meter_number)
                        ?: meter.monthly_status
                    val isMeterBlocked = meterStatus == "VERIFIED" || meterStatus == "PENDING"
                    MeterCard(
                        meter = meter,
                        meterStatus = meterStatus,
                        index = index,
                        isSaved = isSaved,
                        isBlocked = isMeterBlocked,
                        powerVa = customer.power_va,
                        onClick = {
                            if (!isSaved && !isMeterBlocked) {
                                viewModel.selectMeter(index)
                                onMeterSelected(index)
                            }
                        }
                    )
                }
            }
    }
}

@Composable
private fun MeterCard(
    meter: Meter,
    meterStatus: String?,
    index: Int,
    isSaved: Boolean,
    isBlocked: Boolean,
    powerVa: Int,
    onClick: () -> Unit
) {
    val isVerified = meterStatus == "VERIFIED"
    val isPending = meterStatus == "PENDING"
    val statusColor = when {
        isVerified || isSaved -> Color(0xFF2E7D32)
        isPending -> Color(0xFFF9A825)
        else -> Color(0xFF1565C0)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSaved && !isBlocked) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSaved || isVerified -> Color(0xFFE8F5E9)
                isPending -> Color(0xFFFFF8E1)
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
                                isSaved || isVerified -> listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
                                isPending -> listOf(Color(0xFFF9A825), Color(0xFFFFC107))
                                else -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSaved || isVerified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isPending) {
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
                val statusLabel = when (meterStatus) {
                    "VERIFIED" -> "Terverifikasi"
                    "PENDING" -> "Pending"
                    else -> "Terblokir"
                }
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "Input ->",
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
