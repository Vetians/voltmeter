package org.ukrida.voltmeter.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ukrida.voltmeter.data.local.AppDatabase
import org.ukrida.voltmeter.data.local.entity.CustomerEntity
import org.ukrida.voltmeter.data.local.entity.MeterDto
import org.ukrida.voltmeter.data.local.entity.MeterRecordEntity
import org.ukrida.voltmeter.data.local.entity.UserEntity
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.data.model.Meter
import org.ukrida.voltmeter.data.model.MeterRecord
import org.ukrida.voltmeter.data.model.User

class LocalRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val customerDao = db.customerDao()
    private val meterRecordDao = db.meterRecordDao()
    private val userDao = db.userDao()

    // ============= AUTH =============
    suspend fun login(username: String, password: String): User? {
        val userEntity = userDao.login(username, password)
        return userEntity?.toDomain()
    }

    suspend fun saveUser(user: User) {
        userDao.insert(user.toEntity())
    }

    // ============= CUSTOMERS =============
    fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getCustomerById(customerId: String): Customer? {
        return customerDao.getCustomerById(customerId)?.toDomain()
    }

    suspend fun saveCustomers(customers: List<Customer>) {
        customerDao.insertAll(customers.map { it.toEntity() })
    }

    suspend fun saveCustomer(customer: Customer) {
        customerDao.insert(customer.toEntity())
    }

    suspend fun deleteAllCustomers() {
        customerDao.deleteAll()
    }

    // ============= METER RECORDS =============
    fun getAllRecords(): Flow<List<MeterRecord>> {
        return meterRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getRecordsByCustomerId(customerId: String): Flow<List<MeterRecord>> {
        return meterRecordDao.getRecordsByCustomerId(customerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getRecordsByUserId(userId: String): Flow<List<MeterRecord>> {
        return meterRecordDao.getRecordsByUserId(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getPendingRecords(userId: String? = null): Flow<List<MeterRecord>> {
        return if (userId != null) {
            meterRecordDao.getRecordsByStatusAndUser("PENDING", userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            meterRecordDao.getRecordsByStatus("PENDING").map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    fun getVerifiedRecords(userId: String? = null): Flow<List<MeterRecord>> {
        return if (userId != null) {
            meterRecordDao.getRecordsByStatusAndUser("VERIFIED", userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            meterRecordDao.getRecordsByStatus("VERIFIED").map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    fun getRejectedRecords(userId: String? = null): Flow<List<MeterRecord>> {
        return if (userId != null) {
            meterRecordDao.getRecordsByStatusAndUser("REJECTED", userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            meterRecordDao.getRecordsByStatus("REJECTED").map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    suspend fun saveRecord(record: MeterRecord) {
        meterRecordDao.insert(record.toEntity())
    }

    suspend fun saveRecords(records: List<MeterRecord>) {
        meterRecordDao.insertAll(records.map { it.toEntity() })
    }

    suspend fun replaceSyncedRecords(records: List<MeterRecord>, recordedBy: String? = null, status: String? = null) {
        if (recordedBy != null && status != null) {
            meterRecordDao.deleteSyncedByUserAndStatus(recordedBy, status)
        } else if (recordedBy != null) {
            meterRecordDao.deleteSyncedByUser(recordedBy)
        } else if (status != null) {
            meterRecordDao.deleteSyncedByStatus(status)
        } else {
            meterRecordDao.deleteAllSynced()
        }
        meterRecordDao.insertAll(records.map { it.toEntity() })
    }

    /**
     * Menghapus SEMUA records (isSynced=0 dan isSynced=1) untuk kombinasi user+status tertentu,
     * lalu insert records terbaru dari server.
     *
     * Digunakan saat load dari server agar records offline lama (isSynced=0) tidak muncul
     * bersamaan dengan versi server → mencegah duplikat pending.
     */
    suspend fun replaceAllRecordsByUserAndStatus(
        records: List<MeterRecord>,
        recordedBy: String? = null,
        status: String? = null
    ) {
        if (recordedBy != null && status != null) {
            meterRecordDao.deleteAllByUserAndStatus(recordedBy, status)
        } else if (recordedBy != null) {
            meterRecordDao.deleteAllByUser(recordedBy)
        } else if (status != null) {
            meterRecordDao.deleteAllByStatus(status)
        } else {
            meterRecordDao.deleteAll()
        }
        meterRecordDao.insertAll(records.map { it.toEntity() })
    }

    suspend fun getUnsyncedRecords(): List<MeterRecord> {
        return meterRecordDao.getUnsyncedRecords().map { it.toDomain() }
    }

    suspend fun markRecordAsSynced(recordId: String) {
        meterRecordDao.markAsSynced(recordId)
    }

    suspend fun updateVerificationStatus(recordId: String, status: String, note: String? = null) {
        meterRecordDao.updateVerificationStatus(recordId, status, note)
    }

    suspend fun deleteAllRecords() {
        meterRecordDao.deleteAll()
    }

    // ============= EXTENSION FUNCTIONS =============
    private fun CustomerEntity.toDomain(): Customer {
        return Customer(
            customer_id = customer_id,
            name = name,
            address = address,
            power_va = power_va,
            tariff = tariff,
            last_month_usage = last_month_usage,
            last_meter_reading = last_meter_reading,
            meters = meters.map { Meter(it.meter_number, it.last_reading) },
            latitude = latitude,
            longitude = longitude,
            monthly_status = monthly_status
        )
    }

    private fun Customer.toEntity(): CustomerEntity {
        return CustomerEntity(
            customer_id = customer_id,
            name = name,
            address = address,
            power_va = power_va,
            tariff = tariff,
            last_month_usage = last_month_usage,
            last_meter_reading = last_meter_reading,
            meters = meters.map { MeterDto(it.meter_number, it.last_reading) },
            latitude = latitude,
            longitude = longitude,
            monthly_status = monthly_status
        )
    }

    private fun MeterRecordEntity.toDomain(): MeterRecord {
        return MeterRecord(
            record_id = record_id,
            customer_id = customer_id,
            meter_number = meter_number,
            previous_reading = previous_reading,
            current_reading = current_reading,
            usage_kwh = usage_kwh,
            record_date = record_date,
            record_time = record_time,
            visit_status = visit_status,
            photo_path = photo_path,
            latitude = latitude,
            longitude = longitude,
            notes = notes,
            recorded_by = recorded_by,
            verification_status = verification_status,
            verification_note = verification_note,
            customer_name = customer_name,
            customer_address = customer_address
        )
    }

    private fun MeterRecord.toEntity(): MeterRecordEntity {
        return MeterRecordEntity(
            record_id = record_id,
            customer_id = customer_id,
            meter_number = meter_number,
            previous_reading = previous_reading,
            current_reading = current_reading,
            usage_kwh = usage_kwh,
            record_date = record_date,
            record_time = record_time,
            visit_status = visit_status,
            photo_path = photo_path,
            latitude = latitude,
            longitude = longitude,
            notes = notes,
            recorded_by = recorded_by,
            verification_status = verification_status,
            verification_note = verification_note,
            customer_name = customer_name,
            customer_address = customer_address,
            isSynced = true
        )
    }

    private fun UserEntity.toDomain(): User {
        return User(
            id = 1,
            user_id = user_id,
            name = name,
            username = username,
            password = password,
            role = role,
            token = token
        )
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(
            user_id = user_id,
            name = name,
            username = username,
            password = password,
            role = role,
            token = token
        )
    }
}
