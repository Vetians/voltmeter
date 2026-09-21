package org.ukrida.voltmeter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.ukrida.voltmeter.data.local.converter.MeterListConverter

@Entity(tableName = "customers")
@TypeConverters(MeterListConverter::class)
data class CustomerEntity(
    @PrimaryKey
    val customer_id: String,
    val name: String,
    val address: String,
    val power_va: Int,
    val tariff: String,
    val last_month_usage: Double,
    val last_meter_reading: Double,
    val meters: List<MeterDto>,
    val latitude: Double,
    val longitude: Double,
    val monthly_status: String?,
    val lastSyncTime: Long = System.currentTimeMillis()
)

data class MeterDto(
    val meter_number: String,
    val last_reading: Double,
    val monthly_status: String? = null
)
