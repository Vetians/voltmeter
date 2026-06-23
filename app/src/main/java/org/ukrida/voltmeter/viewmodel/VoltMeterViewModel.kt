package org.ukrida.voltmeter.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.data.model.User
import org.ukrida.voltmeter.data.repository.VoltMeterRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoltMeterViewModel(private val repo: VoltMeterRepository) : ViewModel() {

    // ============= AUTH STATE =============
    var currentUser = mutableStateOf<User?>(null)
    var loginError = mutableStateOf<String?>(null)
    var isLoggedIn = mutableStateOf(false)

    // ============= CUSTOMER STATE =============
    var customers = mutableStateOf<List<Customer>>(emptyList())
    var selectedCustomer = mutableStateOf<Customer?>(null)

    // ============= METER RECORD STATE =============
    var meterRecords = mutableStateOf<List<MeterRecord>>(emptyList())
    var todayRecords = mutableStateOf<List<MeterRecord>>(emptyList())

    // ============= RECORDING STATE =============
    var currentReading = mutableStateOf("")
    var visitStatus = mutableStateOf("TERBACA_NORMAL")
    var photoPath = mutableStateOf<String?>(null)
    var notes = mutableStateOf("")
    var currentMeterIndex = mutableStateOf(0)

    // ============= MESSAGE STATE =============
    var successMessage = mutableStateOf<String?>(null)
    var errorMessage = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)

    // ============= SYNC STATE =============
    var lastSync = mutableStateOf("Belum pernah sync")

    // ============= AUTH =============
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val user = repo.login(username, password)
                currentUser.value = user
                isLoggedIn.value = true
                loginError.value = null
            } catch (e: Exception) {
                currentUser.value = null
                isLoggedIn.value = false
                loginError.value = "Username atau password salah"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun logout() {
        currentUser.value = null
        isLoggedIn.value = false
        customers.value = emptyList()
        meterRecords.value = emptyList()
    }

    // ============= WORK ORDERS =============
    fun syncWorkOrders() {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                val workOrders = repo.getWorkOrders(token)
                val allCustomers = mutableListOf<Customer>()
                workOrders.forEach { wo ->
                    allCustomers.addAll(wo.customers)
                }
                customers.value = allCustomers

                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                lastSync.value = dateFormat.format(Date())

                successMessage.value = "Berhasil sync ${allCustomers.size} data pelanggan"
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Sync gagal", e)
                errorMessage.value = "Gagal sync data: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // ============= CUSTOMER =============
    fun selectCustomer(customer: Customer) {
        selectedCustomer.value = customer
        currentMeterIndex.value = 0
        currentReading.value = ""
        photoPath.value = null
        notes.value = ""
        visitStatus.value = "TERBACA_NORMAL"
    }

    // ============= RECORDING =============
    fun setCurrentReading(reading: String) {
        currentReading.value = reading
    }

    fun setVisitStatus(status: String) {
        visitStatus.value = status
    }

    fun setPhotoPath(path: String?) {
        photoPath.value = path
    }

    fun setNotes(n: String) {
        notes.value = n
    }

    fun submitMeterRecord() {
        val token = currentUser.value?.token ?: return
        val customer = selectedCustomer.value ?: return
        val reading = currentReading.value.toDoubleOrNull()

        if (reading == null) {
            errorMessage.value = "Masukkan angka meter yang valid"
            return
        }

        val meter = customer.meters.getOrNull(currentMeterIndex.value)

        viewModelScope.launch {
            try {
                isLoading.value = true
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                val record = mapOf(
                    "customer_id" to customer.customer_id,
                    "meter_number" to (meter?.meter_number ?: ""),
                    "previous_reading" to (meter?.last_reading ?: 0.0),
                    "current_reading" to reading,
                    "record_date" to dateFormat.format(Date()),
                    "record_time" to timeFormat.format(Date()),
                    "visit_status" to visitStatus.value,
                    "latitude" to customer.latitude,
                    "longitude" to customer.longitude,
                    "notes" to notes.value
                )

                repo.submitMeterRecord(token, record)
                successMessage.value = "Pencatatan berhasil disimpan"

                // Reset state
                currentReading.value = ""
                photoPath.value = null
                notes.value = ""

            } catch (e: Exception) {
                Log.e("VOLTMETER", "Submit gagal", e)
                errorMessage.value = "Gagal menyimpan: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // ============= HISTORY =============
    fun loadMeterRecords() {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                meterRecords.value = repo.getMeterRecords(token)
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load records gagal", e)
            }
        }
    }

    fun loadTodayRecords() {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                todayRecords.value = repo.getTodayRecords(token)
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load today records gagal", e)
            }
        }
    }

    fun clearMessages() {
        successMessage.value = null
        errorMessage.value = null
    }
}
