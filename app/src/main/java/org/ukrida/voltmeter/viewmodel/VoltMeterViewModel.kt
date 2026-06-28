package org.ukrida.voltmeter.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.data.model.StatsResponse
import org.ukrida.voltmeter.data.model.SubmitRecordRequest
import org.ukrida.voltmeter.data.model.User
import org.ukrida.voltmeter.data.repository.VoltMeterRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoltMeterViewModel(private val repo: VoltMeterRepository) : ViewModel() {

    // ============= AUTH STATE =============
    var currentUser = mutableStateOf<User?>(null)
    var loginError = mutableStateOf<String?>(null)
    var isLoggedIn = mutableStateOf(false)

    // ============= ADMIN STATE =============
    var adminStats = mutableStateOf(StatsResponse())
    var adminUsersList = mutableStateOf<List<User>>(emptyList())
    var selectedAdminMonth = mutableStateOf<Int?>(null) // null means All Time
    var selectedAdminYear = mutableStateOf<Int?>(null)

    // ============= CUSTOMER STATE =============
    var customers = mutableStateOf<List<Customer>>(emptyList())
    var selectedCustomer = mutableStateOf<Customer?>(null)
    
    // For Admin Customer Detail
    var selectedAdminCustomer = mutableStateOf<Customer?>(null)
    var customerHistory = mutableStateOf<List<MeterRecord>>(emptyList())

    // ============= METER RECORD STATE =============
    var meterRecords = mutableStateOf<List<MeterRecord>>(emptyList())
    var todayRecords = mutableStateOf<List<MeterRecord>>(emptyList())

    // ============= RECORDING STATE =============
    var currentReading = mutableStateOf("")
    var visitStatus = mutableStateOf("TERBACA_NORMAL")
    var photoUriString = mutableStateOf<String?>(null)
    var photoFile = mutableStateOf<File?>(null) // Save the actual file reference
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
        adminUsersList.value = emptyList()
    }

    // ============= ADMIN =============
    fun loadAdminData() {
        val token = currentUser.value?.token ?: return
        
        // Load Stats
        viewModelScope.launch {
            try {
                adminStats.value = repo.getStatistics(token, selectedAdminMonth.value, selectedAdminYear.value)
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load admin stats gagal", e)
            }
        }

        // Load Users
        viewModelScope.launch {
            try {
                adminUsersList.value = repo.getUsers(token)
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load admin users gagal", e)
            }
        }
    }

    fun insertUser(user: User) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repo.insertUser(token, user)
                successMessage.value = "Pengguna berhasil ditambahkan"
                // Reload list after insert
                loadAdminData()
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Insert user gagal", e)
                errorMessage.value = "Gagal menambah pengguna: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateUser(user: User) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                val response = repo.updateUser(token, user)
                if (response.success) {
                    successMessage.value = "Pengguna berhasil diperbarui"
                    loadAdminData()
                } else {
                    errorMessage.value = response.message
                }
            } catch (e: Exception) {
                errorMessage.value = "Gagal memperbarui pengguna: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addCustomer(customer: Customer) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                val response = repo.addCustomer(token, customer)
                if (response.success) {
                    successMessage.value = "Pelanggan baru berhasil ditambahkan"
                    loadAllCustomers()
                } else {
                    errorMessage.value = response.message
                }
            } catch (e: Exception) {
                errorMessage.value = "Gagal menambah pelanggan: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun verifyRecord(recordId: String, status: String, note: String? = null, customerId: String) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                val request = org.ukrida.voltmeter.data.model.VerifyRequest(recordId, status, note)
                val response = repo.verifyRecord(token, request)
                if (response.success) {
                    successMessage.value = "Status verifikasi berhasil diperbarui"
                    // Reload customer history
                    loadCustomerHistory(customerId)
                } else {
                    errorMessage.value = response.message
                }
            } catch (e: Exception) {
                errorMessage.value = "Gagal memverifikasi data: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadAllCustomers() {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                customers.value = repo.getCustomers(token)
                
                // If a customer is currently selected, refresh its data
                selectedAdminCustomer.value?.let { selected ->
                    selectedAdminCustomer.value = customers.value.find { it.customer_id == selected.customer_id } ?: selected
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load all customers gagal", e)
                errorMessage.value = "Gagal memuat pelanggan: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadCustomerHistory(customerId: String) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                customerHistory.value = repo.getMeterRecords(token, customerId)
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load history gagal", e)
            }
        }
    }

    fun addMeter(customerId: String, meterNumber: String) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repo.addMeter(token, customerId, meterNumber)
                successMessage.value = "Meteran berhasil ditambahkan"
                loadAllCustomers() // Reload to get updated meters
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Add meter gagal", e)
                errorMessage.value = "Gagal menambah meteran: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun deleteMeter(meterNumber: String) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                repo.deleteMeter(token, meterNumber)
                successMessage.value = "Meteran berhasil dihapus"
                loadAllCustomers() // Reload to get updated meters
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Delete meter gagal", e)
                errorMessage.value = "Gagal menghapus meteran: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
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
        photoUriString.value = null
        photoFile.value = null
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

    fun setPhoto(uri: String?, file: File?) {
        photoUriString.value = uri
        photoFile.value = file
    }

    fun setNotes(n: String) {
        notes.value = n
    }

    fun submitMeterRecord(latitude: Double = 0.0, longitude: Double = 0.0) {
        val token = currentUser.value?.token ?: return
        val customer = selectedCustomer.value ?: return
        
        // If "RUMAH_KOSONG", reading can be 0 or empty, handled by the API rules.
        // We'll parse it safely to 0.0 if empty.
        val reading = currentReading.value.toDoubleOrNull() ?: 0.0
        
        val meter = customer.meters.getOrNull(currentMeterIndex.value)
        val pFile = photoFile.value

        if (pFile == null) {
            errorMessage.value = "Foto wajib diambil"
            return
        }

        viewModelScope.launch {
            try {
                isLoading.value = true
                
                // 1. Upload Photo First
                val requestFile = pFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("photo", pFile.name, requestFile)
                val customerIdBody = customer.customer_id.toRequestBody("text/plain".toMediaTypeOrNull())
                
                val uploadResponse = repo.uploadFoto(token, body, customerIdBody)
                
                if (!uploadResponse.success) {
                    errorMessage.value = "Gagal upload foto: ${uploadResponse.message}"
                    isLoading.value = false
                    return@launch
                }

                // 2. Submit Record
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                val record = SubmitRecordRequest(
                    customer_id = customer.customer_id,
                    meter_number = meter?.meter_number ?: "",
                    previous_reading = meter?.last_reading ?: 0.0,
                    current_reading = reading,
                    record_date = dateFormat.format(Date()),
                    record_time = timeFormat.format(Date()),
                    visit_status = visitStatus.value,
                    photo_path = uploadResponse.photo_path,
                    latitude = latitude,
                    longitude = longitude,
                    notes = notes.value,
                    recorded_by = currentUser.value?.user_id ?: ""
                )

                val submitResp = repo.submitMeterRecord(token, record)
                if (submitResp.success) {
                    successMessage.value = "Pencatatan berhasil disimpan"
                    
                    // Reset state
                    currentReading.value = ""
                    photoUriString.value = null
                    photoFile.value = null
                    notes.value = ""
                } else {
                    errorMessage.value = "Gagal menyimpan data ke server."
                }

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
