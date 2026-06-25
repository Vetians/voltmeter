package org.ukrida.voltmeter.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.data.model.StatsResponse
import org.ukrida.voltmeter.data.model.SubmitRecordResponse
import org.ukrida.voltmeter.data.model.UploadResponse
import org.ukrida.voltmeter.data.model.User
import org.ukrida.voltmeter.data.model.WorkOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

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
        @Header("Authorization") token: String,
        @Query("customer_id") customerId: String? = null
    ): List<MeterRecord>

    // ============= METERS (MANAGE) =============
    @POST("meters.php")
    suspend fun addMeter(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Unit>

    @DELETE("meters.php")
    suspend fun deleteMeter(
        @Header("Authorization") token: String,
        @Query("meter_number") meterNumber: String
    ): Response<Unit>

    @POST("meter_records.php")
    suspend fun submitMeterRecord(
        @Header("Authorization") token: String,
        @Body record: Map<String, Any>
    ): SubmitRecordResponse

    @GET("meter_records_today.php")
    suspend fun getTodayRecords(
        @Header("Authorization") token: String
    ): List<MeterRecord>

    // ============= UPLOAD FOTO =============
    @Multipart
    @POST("upload.php")
    suspend fun uploadFoto(
        @Header("Authorization") token: String,
        @Part photo: MultipartBody.Part,
        @Part("customer_id") customerId: RequestBody
    ): UploadResponse

    // ============= ADMIN ENDPOINTS =============
    @GET("kunjungan.php?statistik=1")
    suspend fun getStatistics(
        @Header("Authorization") token: String
    ): StatsResponse

    @GET("users.php")
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): List<User>

    @POST("users.php")
    suspend fun insertUser(
        @Header("Authorization") token: String,
        @Body user: User
    ): retrofit2.Response<Unit>
}
