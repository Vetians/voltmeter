package org.ukrida.voltmeter.data.api

import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.data.model.User
import org.ukrida.voltmeter.data.model.WorkOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    // ============= AUTH =============
    @POST("login.php")
    suspend fun login(
        @Body request: Map<String, String>
    ): User

    // ============= WORK ORDERS =============
    @GET("work_orders.php")
    suspend fun getWorkOrders(
        @Header("Authorization") token: String
    ): List<WorkOrderResponse>

    // ============= CUSTOMERS =============
    @GET("customers.php")
    suspend fun getCustomers(
        @Header("Authorization") token: String
    ): List<Customer>

    // ============= METER RECORDS =============
    @GET("meter_records.php")
    suspend fun getMeterRecords(
        @Header("Authorization") token: String
    ): List<MeterRecord>

    @POST("meter_records.php")
    suspend fun submitMeterRecord(
        @Header("Authorization") token: String,
        @Body record: Map<String, Any>
    ): Response<Unit>

    @GET("meter_records_today.php")
    suspend fun getTodayRecords(
        @Header("Authorization") token: String
    ): List<MeterRecord>
}
