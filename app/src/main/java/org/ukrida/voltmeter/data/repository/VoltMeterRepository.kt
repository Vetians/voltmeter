package org.ukrida.voltmeter.data.repository

import org.ukrida.voltmeter.data.api.ApiService
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
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
    suspend fun getMeterRecords(token: String): List<MeterRecord> {
        return api.getMeterRecords("Bearer $token")
    }

    suspend fun submitMeterRecord(token: String, record: Map<String, Any>) {
        api.submitMeterRecord("Bearer $token", record)
    }

    suspend fun getTodayRecords(token: String): List<MeterRecord> {
        return api.getTodayRecords("Bearer $token")
    }
}
