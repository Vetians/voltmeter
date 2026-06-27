package org.ukrida.voltmeter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@Composable
fun HistoryScreen(viewModel: VoltMeterViewModel) {
    val records = viewModel.meterRecords.value

    LaunchedEffect(Unit) {
        viewModel.loadMeterRecords()
    }

    if (records.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Belum ada riwayat pencatatan",
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 16.dp,
                bottom = 16.dp
            )
        ) {
            items(records) { record ->
                HistoryCard(record = record)
            }
        }
    }
}

@Composable
fun HistoryCard(record: MeterRecord) {
    val statusColor = when (record.visit_status) {
        "TERBACA_NORMAL" -> Color(0xFF4CAF50)
        "RUMAH_KOSONG" -> Color(0xFFFF9800)
        "HALANGAN" -> Color(0xFFF44336)
        else -> Color.Gray
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.customer_id,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(
                            statusColor.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.visit_status.replace("_", " "),
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Meter: ${record.meter_number}",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.previous_reading} → ${record.current_reading} kWh",
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (record.is_verified == 1) Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else Color(0xFFFF9800).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (record.is_verified == 1) "Selesai" else "Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.is_verified == 1) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
            Text(
                text = "Pemakaian: ${String.format("%.2f", record.usage_kwh)} kWh",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.record_date,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = record.record_time,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (record.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan: ${record.notes}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
