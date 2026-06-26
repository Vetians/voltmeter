package org.ukrida.voltmeter.data.repository

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.ukrida.voltmeter.data.api.ApiService
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.data.model.StatsResponse
import org.ukrida.voltmeter.data.model.SubmitRecordRequest
import org.ukrida.voltmeter.data.model.SubmitRecordResponse
import org.ukrida.voltmeter.data.model.UploadResponse
import org.ukrida.voltmeter.data.model.User
import org.ukrida.voltmeter.data.model.WorkOrderResponse

class VoltMeterRepository(private val api: ApiService) {

    // ============= AUTH =============
    suspend fun login(username: String, password: String): User {
        return api.login(
            request = mapOf(
                "username" to username,
                "password" to password
            )
        )
    }

    // ============= WORK ORDERS =============
    suspend fun getWorkOrders(token: String): List<WorkOrderResponse> {
        return api.getWorkOrders("Bearer $token")
    }

    // ============= CUSTOMERS =============
    suspend fun getCustomers(token: String): List<Customer> {
        return api.getCustomers("Bearer $token")
    }

    // ============= METER RECORDS =============
    suspend fun getMeterRecords(token: String, customerId: String? = null): List<MeterRecord> {
        return api.getMeterRecords("Bearer $token", customerId)
    }

    suspend fun submitMeterRecord(token: String, record: SubmitRecordRequest): SubmitRecordResponse {
        return api.submitMeterRecord("Bearer $token", record)
    }

    // ============= METERS (MANAGE) =============
    suspend fun addMeter(token: String, customerId: String, meterNumber: String) {
        val request = mapOf(
            "customer_id" to customerId,
            "meter_number" to meterNumber
        )
        api.addMeter("Bearer $token", request)
    }

    suspend fun deleteMeter(token: String, meterNumber: String) {
        api.deleteMeter("Bearer $token", meterNumber)
    }

    suspend fun getTodayRecords(token: String): List<MeterRecord> {
        return api.getTodayRecords("Bearer $token")
    }

    // ============= UPLOAD FOTO =============
    suspend fun uploadFoto(token: String, photo: MultipartBody.Part, customerId: RequestBody): UploadResponse {
        return api.uploadFoto("Bearer $token", photo, customerId)
    }

    // ============= ADMIN =============
    suspend fun getStatistics(token: String): StatsResponse {
        return api.getStatistics("Bearer $token")
    }

    suspend fun getUsers(token: String): List<User> {
        return api.getUsers("Bearer $token")
    }

    suspend fun insertUser(token: String, user: User) {
        api.insertUser("Bearer $token", user)
    }
}
