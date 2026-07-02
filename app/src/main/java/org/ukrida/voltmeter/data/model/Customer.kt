package org.ukrida.voltmeter.data.model

data class Customer(
    val customer_id: String = "",
    val name: String = "",
    val address: String = "",
    val power_va: Int = 0,
    val tariff: String = "",
    val last_month_usage: Double = 0.0,
    val last_meter_reading: Double = 0.0,
    val meters: List<Meter> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val monthly_status: String? = null
)

data class Meter(
    val meter_number: String = "",
    val last_reading: Double = 0.0
)

data class WorkOrderResponse(
    val work_order_id: String = "",
    val customers: List<Customer> = emptyList()
)
