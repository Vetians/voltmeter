package org.ukrida.voltmeter.data.model

data class MeterRecord(
    val id: Int = 0,
    val record_id: String = "",
    val customer_id: String = "",
    val meter_number: String = "",
    val previous_reading: Double = 0.0,
    val current_reading: Double = 0.0,
    val usage_kwh: Double = 0.0,
    val record_date: String = "",
    val record_time: String = "",
    val visit_status: String = "TERBACA_NORMAL",
    val photo_path: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val notes: String = "",
    val recorded_by: String = "",
    val is_verified: Int = 0,
    val verified_by: String = "",
    val customer_name: String = "",
    val customer_address: String = ""
)

data class SubmitRecordRequest(
    val customer_id: String = "",
    val meter_number: String = "",
    val previous_reading: Double = 0.0,
    val current_reading: Double = 0.0,
    val record_date: String = "",
    val record_time: String = "",
    val visit_status: String = "TERBACA_NORMAL",
    val photo_path: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val notes: String = "",
    val recorded_by: String = ""
)
