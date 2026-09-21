package org.ukrida.voltmeter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meter_records")
data class MeterRecordEntity(
    @PrimaryKey
    val record_id: String,
    val customer_id: String,
    val meter_number: String,
    val previous_reading: Double,
    val current_reading: Double,
    val usage_kwh: Double,
    val record_date: String,
    val record_time: String,
    val visit_status: String,
    val photo_path: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String,
    val recorded_by: String,
    val verification_status: String,
    val verification_note: String?,
    val customer_name: String,
    val customer_address: String,
    val isSynced: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis()
)
