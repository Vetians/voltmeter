package org.ukrida.voltmeter.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
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
import org.ukrida.voltmeter.data.repository.LocalRepository
import org.ukrida.voltmeter.data.repository.VoltMeterRepository
import java.io.File
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoltMeterViewModel(
    private val repo: VoltMeterRepository,
    private val localRepo: LocalRepository,
    private val context: Context
) : ViewModel() {

    // ============= AUTH STATE =============
    var currentUser = mutableStateOf<User?>(null)
    var loginError = mutableStateOf<String?>(null)
    var isLoggedIn = mutableStateOf(false)

    // ============= ADMIN STATE =============
    var adminStats = mutableStateOf(StatsResponse())
    var adminUsersList = mutableStateOf<List<User>>(emptyList())
    var selectedAdminMonth = mutableStateOf<Int?>(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
    var selectedAdminYear = mutableStateOf<Int?>(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))

    // ============= SURVEYOR FILTER STATE =============
    var selectedSurveyorMonth = mutableStateOf<Int?>(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
    var selectedSurveyorYear = mutableStateOf<Int?>(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))

    // ============= PENDING / VERIFIED / REJECTED STATE =============
    var pendingRecords = mutableStateOf<List<MeterRecord>>(emptyList())
    var verifiedRecords = mutableStateOf<List<MeterRecord>>(emptyList())
    var rejectedRecords = mutableStateOf<List<MeterRecord>>(emptyList())

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
    var photoFile = mutableStateOf<File?>(null)
    var notes = mutableStateOf("")
    var currentMeterIndex = mutableStateOf(0)
    var savedMeters = mutableStateOf<Set<Int>>(emptySet())

    // ============= MESSAGE STATE =============
    var successMessage = mutableStateOf<String?>(null)
    var errorMessage = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)

    // ============= SYNC STATE =============
    var lastSync = mutableStateOf("Belum pernah sync")
    var isOnline = mutableStateOf(true)

    // ============= AUTH =============
    // Default credentials untuk internal testing (tanpa hosting database)
    // Format: username -> Triple(password, role, user_id)
    private val defaultUsers = mapOf(
        "admin" to Triple("admin123", "admin", "USR001"),
        "surveyor1" to Triple("surveyor123", "surveyor", "USR002"),
        "surveyor2" to Triple("surveyor123", "surveyor", "USR003")
    )

    init {
        // Load data dari local database saat startup
        loadLocalData()
    }

    private fun loadLocalData() {
        viewModelScope.launch {
            localRepo.getAllCustomers().collectLatest { localCustomers ->
                if (localCustomers.isNotEmpty()) {
                    customers.value = localCustomers
                }
            }
        }

        viewModelScope.launch {
            localRepo.getAllRecords().collectLatest { localRecords ->
                if (localRecords.isNotEmpty()) {
                    meterRecords.value = localRecords
                }
            }
        }

        viewModelScope.launch {
            localRepo.getPendingRecords().collectLatest { records ->
                pendingRecords.value = records
            }
        }

        viewModelScope.launch {
            localRepo.getVerifiedRecords().collectLatest { records ->
                verifiedRecords.value = records
            }
        }

        viewModelScope.launch {
            localRepo.getRejectedRecords().collectLatest { records ->
                rejectedRecords.value = records
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true

                // 1. Coba login dari local database dulu
                val localUser = localRepo.login(username, password)
                if (localUser != null) {
                    currentUser.value = localUser
                    isLoggedIn.value = true
                    loginError.value = null
                    isLoading.value = false
                    return@launch
                }

                // 2. Jika tidak ada di local, coba login hardcoded
                val userInfo = defaultUsers[username]
                if (userInfo != null && userInfo.first == password) {
                    val user = User(
                        id = 1,
                        user_id = userInfo.third, // USR001, USR002, USR003
                        name = username.replaceFirstChar { it.uppercase() },
                        username = username,
                        password = password,
                        role = userInfo.second,
                        token = "dummy_token_$username"
                    )
                    currentUser.value = user
                    isLoggedIn.value = true
                    loginError.value = null

                    // Simpan user ke local database
                    localRepo.saveUser(user)
                } else {
                    // 3. Jika tidak ada di hardcoded, coba login via API
                    isOnline.value = checkConnectivity()
                    if (isOnline.value) {
                        try {
                            val apiUser = repo.login(username, password)
                            currentUser.value = apiUser
                            isLoggedIn.value = true
                            loginError.value = null

                            // Simpan user ke local database
                            localRepo.saveUser(apiUser)
                        } catch (e: Exception) {
                            // Login via API gagal
                            currentUser.value = null
                            isLoggedIn.value = false
                            loginError.value = "Username atau password salah"
                        }
                    } else {
                        // Offline dan tidak ada di local/hardcoded
                        currentUser.value = null
                        isLoggedIn.value = false
                        loginError.value = "Username atau password salah (offline)"
                    }
                }
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

    // ============= CHECK CONNECTIVITY =============
    private fun checkConnectivity(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities != null
        } catch (e: Exception) {
            false
        }
    }

    // ============= ADMIN =============
    fun loadAdminData() {
        val token = currentUser.value?.token ?: return

        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    adminStats.value = repo.getStatistics(token, selectedAdminMonth.value, selectedAdminYear.value)
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load admin stats gagal", e)
                isOnline.value = false
            }
        }

        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    adminUsersList.value = repo.getUsers(token)
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load admin users gagal", e)
                isOnline.value = false
            }
        }
    }

    // ============= PENDING / VERIFIED / REJECTED =============
    fun loadPendingRecords(recordedBy: String? = null) {
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getRecordsByVerification(token, 0, recordedBy)
                    localRepo.saveRecords(remoteRecords)
                }
                // Load dari local database
                localRepo.getPendingRecords(recordedBy).collectLatest { records ->
                    pendingRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load pending records gagal", e)
                isOnline.value = false
                // Tetap load dari local
                localRepo.getPendingRecords(recordedBy).collectLatest { records ->
                    pendingRecords.value = records
                }
            }
        }
    }

    fun loadVerifiedRecords(recordedBy: String? = null) {
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getRecordsByVerification(token, 1, recordedBy)
                    localRepo.saveRecords(remoteRecords)
                }
                localRepo.getVerifiedRecords(recordedBy).collectLatest { records ->
                    verifiedRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load verified records gagal", e)
                isOnline.value = false
                localRepo.getVerifiedRecords(recordedBy).collectLatest { records ->
                    verifiedRecords.value = records
                }
            }
        }
    }

    fun loadRejectedRecords(recordedBy: String? = null) {
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getRecordsByVerification(token, 2, recordedBy)
                    localRepo.saveRecords(remoteRecords)
                }
                localRepo.getRejectedRecords(recordedBy).collectLatest { records ->
                    rejectedRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load rejected records gagal", e)
                isOnline.value = false
                localRepo.getRejectedRecords(recordedBy).collectLatest { records ->
                    rejectedRecords.value = records
                }
            }
        }
    }

    fun verifyRecord(recordId: String) {
        val token = currentUser.value?.token ?: return
        val userId = currentUser.value?.user_id ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    repo.verifyRecord(token, recordId, userId)
                }
                successMessage.value = "Pekerjaan berhasil diverifikasi"
                loadPendingRecords()
                loadVerifiedRecords()
                loadRejectedRecords()
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Verify record gagal", e)
                errorMessage.value = "Gagal verifikasi: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun insertUser(user: User) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    repo.insertUser(token, user)
                }
                localRepo.saveUser(user)
                successMessage.value = "Pengguna berhasil ditambahkan"
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
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val response = repo.updateUser(token, user)
                    if (!response.success) {
                        errorMessage.value = response.message
                        isLoading.value = false
                        return@launch
                    }
                }
                localRepo.saveUser(user)
                successMessage.value = "Pengguna berhasil diperbarui"
                loadAdminData()
            } catch (e: Exception) {
                errorMessage.value = "Gagal memperbarui pengguna: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun deleteUser(id: Int) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val response = repo.deleteUser(token, id)
                    if (!response.success) {
                        errorMessage.value = response.message
                        isLoading.value = false
                        return@launch
                    }
                }
                successMessage.value = "Pengguna berhasil dihapus"
                loadAdminData()
            } catch (e: Exception) {
                errorMessage.value = "Gagal menghapus pengguna: ${e.message}"
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
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val response = repo.addCustomer(token, customer)
                    if (!response.success) {
                        errorMessage.value = response.message
                        isLoading.value = false
                        return@launch
                    }
                }
                localRepo.saveCustomer(customer)
                successMessage.value = "Pelanggan baru berhasil ditambahkan"
                loadAllCustomers()
            } catch (e: Exception) {
                errorMessage.value = "Gagal menambah pelanggan: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val response = repo.updateCustomer(token, customer)
                    if (!response.success) {
                        errorMessage.value = response.message
                        isLoading.value = false
                        return@launch
                    }
                }
                localRepo.saveCustomer(customer)
                successMessage.value = "Pelanggan berhasil diperbarui"
                loadAllCustomers()
            } catch (e: Exception) {
                errorMessage.value = "Gagal memperbarui pelanggan: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val response = repo.deleteCustomer(token, customerId)
                    if (!response.success) {
                        errorMessage.value = response.message
                        isLoading.value = false
                        return@launch
                    }
                }
                successMessage.value = "Pelanggan berhasil dihapus"
                loadAllCustomers()
            } catch (e: Exception) {
                errorMessage.value = "Gagal menghapus pelanggan: ${e.message}"
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
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val request = org.ukrida.voltmeter.data.model.VerifyRequest(recordId, status, note)
                    val response = repo.verifyRecord(token, request)
                    if (!response.success) {
                        errorMessage.value = response.message
                        isLoading.value = false
                        return@launch
                    }
                }
                successMessage.value = "Status verifikasi berhasil diperbarui"
                loadCustomerHistory(customerId)
                loadPendingRecords()
                loadVerifiedRecords()
                loadRejectedRecords()
            } catch (e: Exception) {
                errorMessage.value = "Gagal memverifikasi data: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadAllCustomers() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()

                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteCustomers = repo.getCustomers(token)
                    localRepo.saveCustomers(remoteCustomers)
                }

                // Load dari local database
                localRepo.getAllCustomers().collectLatest { localCustomers ->
                    customers.value = localCustomers
                    selectedAdminCustomer.value?.let { selected ->
                        selectedAdminCustomer.value = customers.value.find { it.customer_id == selected.customer_id } ?: selected
                    }
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load all customers gagal", e)
                errorMessage.value = "Gagal memuat pelanggan: ${e.message}"
                isOnline.value = false
                // Tetap load dari local
                localRepo.getAllCustomers().collectLatest { localCustomers ->
                    customers.value = localCustomers
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadCustomerHistory(customerId: String) {
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getMeterRecords(token, customerId)
                    localRepo.saveRecords(remoteRecords)
                }
                localRepo.getRecordsByCustomerId(customerId).collectLatest { records ->
                    customerHistory.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load history gagal", e)
                isOnline.value = false
                localRepo.getRecordsByCustomerId(customerId).collectLatest { records ->
                    customerHistory.value = records
                }
            }
        }
    }

    fun addMeter(customerId: String, meterNumber: String) {
        val token = currentUser.value?.token ?: return
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    repo.addMeter(token, customerId, meterNumber)
                }
                successMessage.value = "Meteran berhasil ditambahkan"
                loadAllCustomers()
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
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    repo.deleteMeter(token, meterNumber)
                }
                successMessage.value = "Meteran berhasil dihapus"
                loadAllCustomers()
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
        viewModelScope.launch {
            try {
                isLoading.value = true
                isOnline.value = checkConnectivity()

                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val workOrders = repo.getWorkOrders(token)
                    val allCustomers = mutableListOf<Customer>()
                    workOrders.forEach { wo ->
                        allCustomers.addAll(wo.customers)
                    }

                    // Simpan ke local database
                    localRepo.saveCustomers(allCustomers)
                    customers.value = allCustomers

                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id"))
                    lastSync.value = dateFormat.format(Date())

                    successMessage.value = "Berhasil sync ${allCustomers.size} data pelanggan"
                } else {
                    errorMessage.value = "Tidak ada koneksi internet. Menampilkan data lokal."
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Sync gagal", e)
                isOnline.value = false
                errorMessage.value = "Gagal sync data: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // ============= SYNC UNSYNCED RECORDS =============
    fun syncUnsyncedRecords() {
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (!isOnline.value) {
                    return@launch
                }

                val unsyncedRecords = localRepo.getUnsyncedRecords()
                if (unsyncedRecords.isEmpty()) {
                    return@launch
                }

                val token = currentUser.value?.token ?: return@launch
                var syncCount = 0

                unsyncedRecords.forEach { record ->
                    try {
                        val submitRequest = SubmitRecordRequest(
                            customer_id = record.customer_id,
                            meter_number = record.meter_number,
                            previous_reading = record.previous_reading,
                            current_reading = record.current_reading,
                            record_date = record.record_date,
                            record_time = record.record_time,
                            visit_status = record.visit_status,
                            photo_path = record.photo_path,
                            latitude = record.latitude,
                            longitude = record.longitude,
                            notes = record.notes,
                            recorded_by = record.recorded_by
                        )
                        val response = repo.submitMeterRecord(token, submitRequest)
                        if (response.success) {
                            localRepo.markRecordAsSynced(record.record_id)
                            syncCount++
                        }
                    } catch (e: Exception) {
                        Log.e("VOLTMETER", "Sync record gagal: ${record.record_id}", e)
                    }
                }

                if (syncCount > 0) {
                    successMessage.value = "Berhasil sync $syncCount record"
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Sync unsynced records gagal", e)
            }
        }
    }

    // ============= MULTI-METER =============
    fun hasMoreMeters(): Boolean {
        val customer = selectedCustomer.value ?: return false
        return currentMeterIndex.value < customer.meters.size - 1
    }

    fun advanceToNextMeter() {
        val customer = selectedCustomer.value ?: return
        if (currentMeterIndex.value < customer.meters.size - 1) {
            currentMeterIndex.value = currentMeterIndex.value + 1
            clearCurrentMeterInput()
        }
    }

    fun selectMeter(index: Int) {
        if (index != currentMeterIndex.value) {
            currentMeterIndex.value = index
            clearCurrentMeterInput()
        }
    }

    fun markMeterSaved(index: Int) {
        savedMeters.value = savedMeters.value + index
        clearCurrentMeterInput()
    }

    private fun clearCurrentMeterInput() {
        currentReading.value = ""
        photoUriString.value = null
        photoFile.value = null
        notes.value = ""
    }

    // ============= RECORDING RULES =============
    fun canRecord(customer: Customer): Boolean {
        return customer.monthly_status == null || customer.monthly_status == "REJECTED"
    }

    fun getRecordBlockReason(customer: Customer): String? {
        return when (customer.monthly_status) {
            "VERIFIED" -> "Pencatatan bulan ini sudah terverifikasi. Tunggu bulan berikutnya."
            "PENDING" -> "Pencatatan bulan ini sedang menunggu verifikasi admin."
            else -> null
        }
    }

    // ============= CUSTOMER =============
    fun selectCustomer(customer: Customer) {
        selectedCustomer.value = customer
        currentMeterIndex.value = 0
        savedMeters.value = emptySet()
        clearCurrentMeterInput()
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
        val customer = selectedCustomer.value ?: return
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
                isOnline.value = checkConnectivity()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                var photoPath = ""

                // Upload foto jika online
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: ""
                    val requestFile = pFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("photo", pFile.name, requestFile)
                    val customerIdBody = customer.customer_id.toRequestBody("text/plain".toMediaTypeOrNull())

                    try {
                        val uploadResponse = repo.uploadFoto(token, body, customerIdBody)
                        if (uploadResponse.success) {
                            photoPath = uploadResponse.photo_path
                        } else {
                            // Jika upload gagal, simpan path lokal
                            photoPath = pFile.absolutePath
                        }
                    } catch (e: Exception) {
                        // Jika gagal upload, simpan path lokal
                        photoPath = pFile.absolutePath
                    }
                } else {
                    // Offline: simpan path lokal
                    photoPath = pFile.absolutePath
                }

                val record = MeterRecord(
                    record_id = "record_${System.currentTimeMillis()}",
                    customer_id = customer.customer_id,
                    meter_number = meter?.meter_number ?: "",
                    previous_reading = meter?.last_reading ?: 0.0,
                    current_reading = reading,
                    usage_kwh = reading - (meter?.last_reading ?: 0.0),
                    record_date = dateFormat.format(Date()),
                    record_time = timeFormat.format(Date()),
                    visit_status = visitStatus.value,
                    photo_path = photoPath,
                    latitude = latitude,
                    longitude = longitude,
                    notes = notes.value,
                    recorded_by = currentUser.value?.user_id ?: "",
                    verification_status = if (isOnline.value) "PENDING" else "PENDING",
                    verification_note = null,
                    customer_name = customer.name,
                    customer_address = customer.address
                )

                // Simpan ke local database
                localRepo.saveRecord(record)

                // Coba sync ke server jika online
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: ""
                    val submitRequest = SubmitRecordRequest(
                        customer_id = customer.customer_id,
                        meter_number = meter?.meter_number ?: "",
                        previous_reading = meter?.last_reading ?: 0.0,
                        current_reading = reading,
                        record_date = dateFormat.format(Date()),
                        record_time = timeFormat.format(Date()),
                        visit_status = visitStatus.value,
                        photo_path = photoPath,
                        latitude = latitude,
                        longitude = longitude,
                        notes = notes.value,
                        recorded_by = currentUser.value?.user_id ?: ""
                    )
                    val submitResp = repo.submitMeterRecord(token, submitRequest)
                    if (submitResp.success) {
                        localRepo.markRecordAsSynced(record.record_id)
                        successMessage.value = "Pencatatan berhasil disimpan dan disync"
                    } else {
                        successMessage.value = "Pencatatan berhasil disimpan (akan disync nanti)"
                    }
                } else {
                    successMessage.value = "Pencatatan berhasil disimpan (offline)"
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
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getMeterRecords(token)
                    localRepo.saveRecords(remoteRecords)
                }
                localRepo.getAllRecords().collectLatest { records ->
                    meterRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load records gagal", e)
                isOnline.value = false
                localRepo.getAllRecords().collectLatest { records ->
                    meterRecords.value = records
                }
            }
        }
    }

    fun loadTodayRecords() {
        viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getTodayRecords(token)
                    localRepo.saveRecords(remoteRecords)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                localRepo.getRecordsByUserId(currentUser.value?.user_id ?: "").collectLatest { records ->
                    todayRecords.value = records.filter { it.record_date == today }
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load today records gagal", e)
                isOnline.value = false
            }
        }
    }

    fun clearMessages() {
        successMessage.value = null
        errorMessage.value = null
    }
}
