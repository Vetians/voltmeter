package org.ukrida.voltmeter.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

    // Job tracking untuk cancel collector lama sebelum buat yang baru
    // Ini mencegah duplikat akibat multiple coroutine subscribe ke Flow yang sama
    private var pendingJob: kotlinx.coroutines.Job? = null
    private var verifiedJob: kotlinx.coroutines.Job? = null
    private var rejectedJob: kotlinx.coroutines.Job? = null
    private var customersJob: kotlinx.coroutines.Job? = null

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
    // Event khusus pencatatan; jangan gunakan successMessage karena pesan sync juga sukses.
    var recordSavedEvent = mutableStateOf(0)
    var verifyingRecordId = mutableStateOf<String?>(null)

    // ============= MESSAGE STATE =============
    var successMessage = mutableStateOf<String?>(null)
    var errorMessage = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)

    // ============= SYNC STATE =============
    var lastSync = mutableStateOf("Belum pernah sync")
    var isOnline = mutableStateOf(true)

    // ============= AUTH =============
    init {
        // Load data dari local database saat startup
        loadLocalData()
    }

    private fun loadLocalData() {
        // Hanya load customers saat startup (data ringan)
        // Records dimuat secara lazy setelah login via loadMeterRecords()/loadPendingRecords()
        // untuk menghindari SQLiteBlobTooBigException pada tabel meter_records yang besar
        viewModelScope.launch {
            localRepo.getAllCustomers().collectLatest { localCustomers ->
                if (localCustomers.isNotEmpty()) {
                    customers.value = localCustomers
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                Log.d("VOLTMETER", "Login attempt: $username")

                // 1. Coba login dari local database dulu (offline mode untuk user yang pernah login)
                val localUser = localRepo.login(username, password)
                if (localUser != null) {
                    Log.d("VOLTMETER", "Login dari local database: ${localUser.user_id}")
                    currentUser.value = localUser
                    isLoggedIn.value = true
                    loginError.value = null
                    isLoading.value = false
                    return@launch
                }

                // 2. Coba login via API (server hosting)
                try {
                    Log.d("VOLTMETER", "Coba login via API ke: ${org.ukrida.voltmeter.data.api.RetrofitInstance.api}")
                    val apiUser = repo.login(username, password)
                    Log.d("VOLTMETER", "API login berhasil: ${apiUser.user_id} - ${apiUser.name}")
                    currentUser.value = apiUser
                    isLoggedIn.value = true
                    loginError.value = null

                    // Simpan user ke local database untuk offline
                    localRepo.saveUser(apiUser)
                    isLoading.value = false
                    return@launch
                } catch (e: Exception) {
                    Log.e("VOLTMETER", "API login gagal: ${e.message}")
                    e.printStackTrace()
                }

                // Jika login di DB lokal & API gagal
                Log.d("VOLTMETER", "Metode login gagal")
                currentUser.value = null
                isLoggedIn.value = false
                loginError.value = "Username atau password salah atau server tidak terjangkau"
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Login error: ${e.message}")
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
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            // Cek apakah punya internet capability
            networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e("VOLTMETER", "Check connectivity error: ${e.message}")
            // Jika gagal cek, asumsikan online untuk mencoba API
            true
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
        // Cancel collector lama dulu untuk mencegah duplikat
        pendingJob?.cancel()
        pendingJob = viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getRecordsByVerification(token, 0, recordedBy)
                    // Hapus semua record PENDING untuk user ini (termasuk yg belum sync)
                    // sebelum replace dengan data terbaru dari server
                    localRepo.replaceAllRecordsByUserAndStatus(remoteRecords, recordedBy, "PENDING")
                }
                localRepo.getPendingRecords(recordedBy).collectLatest { records ->
                    // Deduplikasi berdasarkan record_id untuk jaga-jaga
                    pendingRecords.value = records.distinctBy { it.record_id }
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load pending records gagal", e)
                localRepo.getPendingRecords(recordedBy).collectLatest { records ->
                    pendingRecords.value = records.distinctBy { it.record_id }
                }
            }
        }
    }

    fun loadVerifiedRecords(recordedBy: String? = null) {
        verifiedJob?.cancel()
        verifiedJob = viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getRecordsByVerification(token, 1, recordedBy)
                    localRepo.replaceAllRecordsByUserAndStatus(remoteRecords, recordedBy, "VERIFIED")
                }
                localRepo.getVerifiedRecords(recordedBy).collectLatest { records ->
                    verifiedRecords.value = records.distinctBy { it.record_id }
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load verified records gagal", e)
                localRepo.getVerifiedRecords(recordedBy).collectLatest { records ->
                    verifiedRecords.value = records.distinctBy { it.record_id }
                }
            }
        }
    }

    fun loadRejectedRecords(recordedBy: String? = null) {
        rejectedJob?.cancel()
        rejectedJob = viewModelScope.launch {
            try {
                isOnline.value = checkConnectivity()
                if (isOnline.value) {
                    val token = currentUser.value?.token ?: return@launch
                    val remoteRecords = repo.getRecordsByVerification(token, 2, recordedBy)
                    localRepo.replaceAllRecordsByUserAndStatus(remoteRecords, recordedBy, "REJECTED")
                }
                localRepo.getRejectedRecords(recordedBy).collectLatest { records ->
                    rejectedRecords.value = records.distinctBy { it.record_id }
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load rejected records gagal", e)
                localRepo.getRejectedRecords(recordedBy).collectLatest { records ->
                    rejectedRecords.value = records.distinctBy { it.record_id }
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
                localRepo.updateVerificationStatus(recordId, "VERIFIED")
                successMessage.value = "Pekerjaan berhasil diverifikasi"
                // Admin melihat semua laporan. Jangan filter memakai ID Admin,
                // karena laporan tersebut dibuat oleh surveyor dan akan terlihat
                // seperti "hilang" setelah satu record diproses.
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

    private suspend fun apiError(e: Exception, fallback: String): String {
        if (e is retrofit2.HttpException) {
            val body = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            if (!body.isNullOrBlank()) {
                val msg = try {
                    val json = org.json.JSONObject(body)
                    json.optString("message").ifEmpty { null }
                } catch (_: Exception) {
                    null
                }
                if (!msg.isNullOrBlank()) return msg
                return "$fallback (HTTP ${e.code()})"
            }
            return "$fallback (HTTP ${e.code()})"
        }
        return "$fallback: ${e.message}"
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
                        errorMessage.value = response.message.ifEmpty { "Gagal menambah pelanggan" }
                        isLoading.value = false
                        return@launch
                    }
                }
                localRepo.saveCustomer(customer)
                successMessage.value = "Pelanggan baru berhasil ditambahkan"
                loadAllCustomers()
            } catch (e: Exception) {
                errorMessage.value = apiError(e, "Gagal menambah pelanggan")
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
                errorMessage.value = apiError(e, "Gagal memperbarui pelanggan")
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
        val userId = currentUser.value?.user_id ?: return
        if (verifyingRecordId.value != null) return
        viewModelScope.launch {
            try {
                verifyingRecordId.value = recordId
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
                localRepo.updateVerificationStatus(recordId, status, note)

                // Update monthly_status di customer.meters langsung
                val allLocalRecords = pendingRecords.value + verifiedRecords.value + rejectedRecords.value
                val meterRecord = allLocalRecords.find { it.record_id == recordId }
                    ?: localRepo.getRecordById(recordId)
                if (meterRecord != null) {
                    val updatedCustomers = customers.value.map { cust ->
                        if (cust.customer_id == customerId) {
                            val updatedMeters = cust.meters.map { m ->
                                if (m.meter_number == meterRecord.meter_number) {
                                    // Record yang ditolak tidak boleh mengubah stand dasar.
                                    // Nilai sebelumnya dipulihkan juga pada cache lokal.
                                    m.copy(
                                        last_reading = if (status == "REJECTED") meterRecord.previous_reading else m.last_reading,
                                        monthly_status = status
                                    )
                                } else m
                            }
                            // monthly_status pelanggan hanya ringkasan untuk daftar;
                            // status sebenarnya disimpan dan dievaluasi per meter.
                            val customerStatus = when {
                                updatedMeters.isNotEmpty() && updatedMeters.all { it.monthly_status == "VERIFIED" } -> "VERIFIED"
                                updatedMeters.any { it.monthly_status == "PENDING" } -> "PENDING"
                                updatedMeters.any { it.monthly_status == "REJECTED" } -> "REJECTED"
                                else -> null
                            }
                            cust.copy(meters = updatedMeters, monthly_status = customerStatus)
                        } else cust
                    }
                    customers.value = updatedCustomers
                    // Update selectedCustomer juga
                    selectedCustomer.value?.let { sel ->
                        if (sel.customer_id == customerId) {
                            selectedCustomer.value = updatedCustomers.find { it.customer_id == customerId }
                        }
                    }
                    localRepo.saveCustomers(updatedCustomers)
                }

                successMessage.value = "Status verifikasi berhasil diperbarui"
                loadCustomerHistory(customerId)
                // Admin melihat semua laporan. Jangan filter memakai ID Admin,
                // karena laporan tersebut dibuat oleh surveyor dan akan terlihat
                // seperti "hilang" setelah satu record diproses.
                loadPendingRecords()
                loadVerifiedRecords()
                loadRejectedRecords()
            } catch (e: Exception) {
                errorMessage.value = "Gagal memverifikasi data: ${e.message}"
            } finally {
                isLoading.value = false
                verifyingRecordId.value = null
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

                // Baca SEKALI dengan first(), jangan collectLatest.
                // Flow Room tidak pernah selesai, sehingga blok finally di bawah
                // tidak pernah dijalankan dan isLoading macet -> tombol loading
                // di layar lain (mis. "Tambah Petugas") berputar terus-menerus.
                val localCustomers = localRepo.getAllCustomers().first()
                customers.value = localCustomers
                selectedAdminCustomer.value?.let { selected ->
                    selectedAdminCustomer.value = customers.value.find { it.customer_id == selected.customer_id } ?: selected
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load all customers gagal", e)
                errorMessage.value = "Gagal memuat pelanggan: ${e.message}"
                isOnline.value = false
                // Data lokal tetap tampil lewat kolektor permanen di loadLocalData().
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
                    localRepo.replaceSyncedRecords(remoteRecords)
                }
                localRepo.getRecordsByCustomerId(customerId).collectLatest { records ->
                    customerHistory.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load history gagal", e)
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

                    // Respons work order tidak selalu memuat monthly_status. Jangan sampai
                    // status per meter yang sudah PENDING/VERIFIED di perangkat tertimpa null
                    // setelah pengguna melakukan sync.
                    val existingCustomers = customers.value.associateBy { it.customer_id }
                    val mergedCustomers = allCustomers.map { remoteCustomer ->
                        val localCustomer = existingCustomers[remoteCustomer.customer_id]
                        val localMeters = localCustomer?.meters?.associateBy { it.meter_number }.orEmpty()
                        val mergedMeters = remoteCustomer.meters.map { remoteMeter ->
                            val localMeter = localMeters[remoteMeter.meter_number]
                            remoteMeter.copy(
                                monthly_status = remoteMeter.monthly_status?.ifEmpty { null }
                                    ?: localMeter?.monthly_status
                            )
                        }
                        remoteCustomer.copy(
                            meters = mergedMeters,
                            monthly_status = remoteCustomer.monthly_status?.ifEmpty { null }
                                ?: localCustomer?.monthly_status
                        )
                    }

                    // Simpan data yang telah digabungkan agar status meter tidak reset.
                    localRepo.saveCustomers(mergedCustomers)
                    customers.value = mergedCustomers
                    selectedCustomer.value?.let { selected ->
                        selectedCustomer.value = mergedCustomers.find { it.customer_id == selected.customer_id } ?: selected
                    }

                    // Recompute monthly_status per meter dari data record di DB lokal.
                    // Ini memastikan status VERIFIED/PENDING/REJECTED tetap akurat meskipun
                    // data sync dari server tidak membawa monthly_status.
                    recomputeMeterStatuses()

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

    private suspend fun recomputeMeterStatuses() {
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val updatedCustomers = customers.value.map { customer ->
            val updatedMeters = customer.meters.map { meter ->
                val dbStatus = localRepo.getMeterStatus(customer.customer_id, meter.meter_number, currentYearMonth)
                if (dbStatus != null && dbStatus != meter.monthly_status) {
                    meter.copy(monthly_status = dbStatus)
                } else meter
            }
            val customerStatus = when {
                updatedMeters.all { it.monthly_status == "VERIFIED" } -> "VERIFIED"
                updatedMeters.any { it.monthly_status == "PENDING" } -> "PENDING"
                updatedMeters.any { it.monthly_status == "REJECTED" } -> "REJECTED"
                else -> null
            }
            customer.copy(meters = updatedMeters, monthly_status = customerStatus)
        }
        customers.value = updatedCustomers
        localRepo.saveCustomers(updatedCustomers)
        selectedCustomer.value?.let { sel ->
            selectedCustomer.value = updatedCustomers.find { it.customer_id == sel.customer_id } ?: sel
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
        // Status kartu mengikuti PENDING/VERIFIED dari meter, bukan flag sementara
        // "Selesai". Setelah input tersimpan, pencatatan masih menunggu Admin.
        clearCurrentMeterInput()
    }

    private fun clearCurrentMeterInput() {
        currentReading.value = ""
        photoUriString.value = null
        photoFile.value = null
        notes.value = ""
    }

    // ============= RECORDING RULES =============
    fun getMeterStatus(customerId: String, meterNumber: String): String? {
        if (meterNumber.isEmpty()) return null
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        // Prioritaskan record bulan berjalan di database. Dengan ini status REJECTED
        // dari bulan sebelumnya tidak dapat membuka meter yang VERIFIED bulan ini.
        val statusFromRecord = kotlinx.coroutines.runBlocking {
            localRepo.getMeterStatus(customerId, meterNumber, currentYearMonth)
        }
        if (statusFromRecord != null) return statusFromRecord

        // Fallback ke status per meter yang datang dari hasil sync server.
        val cust = customers.value.find { it.customer_id == customerId }
        val meter = cust?.meters?.find { it.meter_number == meterNumber }
        return meter?.monthly_status
    }

    fun canRecord(customer: Customer, meterIndex: Int = 0): Boolean {
        val meterNumber = customer.meters.getOrNull(meterIndex)?.meter_number ?: return false
        return canRecordMeter(customer, meterNumber)
    }

    fun canRecordMeter(customer: Customer, meterNumber: String): Boolean {
        val status = getMeterStatus(customer.customer_id, meterNumber)
        return status == null || status == "REJECTED"
    }

    fun canRecordAnyMeter(customer: Customer): Boolean {
        return customer.meters.any { canRecordMeter(customer, it.meter_number) }
    }

    fun getRecordBlockReason(customer: Customer, meterIndex: Int = 0): String? {
        val meter = customer.meters.getOrNull(meterIndex) ?: return null
        val meterStatus = getMeterStatus(customer.customer_id, meter.meter_number) ?: meter.monthly_status
        return when (meterStatus) {
            "VERIFIED" -> "Pencatatan meter ${meter.meter_number} bulan ini sudah terverifikasi."
            "PENDING" -> "Pencatatan meter ${meter.meter_number} bulan ini sedang menunggu verifikasi."
            else -> null
        }
    }

    fun getMeterBlockReason(customer: Customer, meterNumber: String): String? {
        val meterStatus = getMeterStatus(customer.customer_id, meterNumber)
            ?: customer.meters.find { it.meter_number == meterNumber }?.monthly_status
        return when (meterStatus) {
            "VERIFIED" -> "Meteran $meterNumber untuk bulan ini sudah terverifikasi oleh Admin. Pencatatan tidak dapat di-input lagi."
            "PENDING" -> "Meteran $meterNumber untuk bulan ini sedang menunggu verifikasi Admin."
            else -> null
        }
    }

    fun getCustomerBlockReason(customer: Customer): String? {
        val blockedMeters = customer.meters.filter {
            val status = getMeterStatus(customer.customer_id, it.meter_number) ?: it.monthly_status
            status == "VERIFIED" || status == "PENDING"
        }
        if (blockedMeters.isEmpty()) return null
        if (blockedMeters.size == customer.meters.size) {
            val allVerified = blockedMeters.all {
                val status = getMeterStatus(customer.customer_id, it.meter_number) ?: it.monthly_status
                status == "VERIFIED"
            }
            return if (allVerified) "Semua meteran sudah terverifikasi bulan ini."
            else "Semua meteran sudah terisi (VERIFIED/PENDING) bulan ini."
        }
        val blockedNames = blockedMeters.map { it.meter_number }.joinToString(", ")
        return "Meteran $blockedNames sudah terverifikasi/pending. Masih ada meteran yang bisa di-input."
    }

    fun getMeterWorkItems(): List<org.ukrida.voltmeter.data.model.MeterWorkItem> {
        val result = mutableListOf<org.ukrida.voltmeter.data.model.MeterWorkItem>()
        customers.value.forEach { customer ->
            if (customer.meters.isNotEmpty()) {
                customer.meters.forEachIndexed { index, meter ->
                    val status = getMeterStatus(customer.customer_id, meter.meter_number)
                    result.add(
                        org.ukrida.voltmeter.data.model.MeterWorkItem(
                            customer = customer,
                            meter = meter,
                            meterIndex = index,
                            meterNumber = meter.meter_number,
                            lastReading = meter.last_reading,
                            status = status
                        )
                    )
                }
            } else {
                val status = getMeterStatus(customer.customer_id, "") ?: customer.monthly_status
                val defaultMeter = org.ukrida.voltmeter.data.model.Meter("MTR-${customer.customer_id}", customer.last_meter_reading)
                result.add(
                    org.ukrida.voltmeter.data.model.MeterWorkItem(
                        customer = customer,
                        meter = defaultMeter,
                        meterIndex = 0,
                        meterNumber = defaultMeter.meter_number,
                        lastReading = defaultMeter.last_reading,
                        status = status
                    )
                )
            }
        }
        return result
    }

    fun selectMeterWorkItem(item: org.ukrida.voltmeter.data.model.MeterWorkItem) {
        selectedCustomer.value = item.customer
        currentMeterIndex.value = item.meterIndex
        savedMeters.value = emptySet()
        clearCurrentMeterInput()
        visitStatus.value = "TERBACA_NORMAL"
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

        if (meter == null) {
            errorMessage.value = "Meter yang dipilih tidak ditemukan"
            return
        }

        if (pFile == null) {
            errorMessage.value = "Foto wajib diambil"
            return
        }

        viewModelScope.launch {
            try {
                isLoading.value = true
                // Validasi ulang dari penyimpanan lokal tepat sebelum simpan. Ini menutup
                // celah ketika halaman input dibuka lalu status meter berubah karena sync.
                val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val latestStatus = localRepo.getMeterStatus(customer.customer_id, meter.meter_number, currentYearMonth)
                    ?: meter.monthly_status
                if (latestStatus == "VERIFIED" || latestStatus == "PENDING") {
                    errorMessage.value = if (latestStatus == "VERIFIED") {
                        "Meteran ${meter.meter_number} sudah terverifikasi dan tidak dapat di-input lagi"
                    } else {
                        "Meteran ${meter.meter_number} sedang menunggu verifikasi Admin"
                    }
                    return@launch
                }
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

                // Update monthly_status di customer.meters langsung ke PENDING
                val updatedCustomers = customers.value.map { cust ->
                    if (cust.customer_id == customer.customer_id) {
                        val updatedMeters = cust.meters.map { m ->
                            if (m.meter_number == record.meter_number) {
                                m.copy(monthly_status = "PENDING")
                            } else m
                        }
                        cust.copy(meters = updatedMeters)
                    } else cust
                }
                customers.value = updatedCustomers
                // Update selectedCustomer juga
                selectedCustomer.value?.let { sel ->
                    if (sel.customer_id == customer.customer_id) {
                        selectedCustomer.value = updatedCustomers.find { it.customer_id == customer.customer_id }
                    }
                }
                localRepo.saveCustomers(updatedCustomers)

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
                recordSavedEvent.value += 1

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
                    localRepo.replaceSyncedRecords(remoteRecords)
                }
                localRepo.getAllRecords().collectLatest { records ->
                    meterRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("VOLTMETER", "Load records gagal", e)
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
                    localRepo.replaceSyncedRecords(remoteRecords, currentUser.value?.user_id)
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