package org.ukrida.voltmeter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.ukrida.voltmeter.data.local.entity.MeterRecordEntity

@Dao
interface MeterRecordDao {
    // Gunakan kolom spesifik (tanpa photo_path) untuk menghindari SQLiteBlobTooBigException
    // photo_path bisa sangat besar (base64/path panjang) dan menyebabkan CursorWindow overflow
    @Query("""
        SELECT record_id, customer_id, meter_number, previous_reading, current_reading,
               usage_kwh, record_date, record_time, visit_status,
               '' AS photo_path,
               latitude, longitude, notes, recorded_by, verification_status,
               verification_note, customer_name, customer_address, isSynced, lastSyncTime
        FROM meter_records
        ORDER BY record_date DESC, record_time DESC
        LIMIT 500
    """)
    fun getAllRecords(): Flow<List<MeterRecordEntity>>

    @Query("""
        SELECT record_id, customer_id, meter_number, previous_reading, current_reading,
               usage_kwh, record_date, record_time, visit_status,
               '' AS photo_path,
               latitude, longitude, notes, recorded_by, verification_status,
               verification_note, customer_name, customer_address, isSynced, lastSyncTime
        FROM meter_records
        WHERE customer_id = :customerId
        ORDER BY record_date DESC
        LIMIT 500
    """)
    fun getRecordsByCustomerId(customerId: String): Flow<List<MeterRecordEntity>>

    @Query("""
        SELECT record_id, customer_id, meter_number, previous_reading, current_reading,
               usage_kwh, record_date, record_time, visit_status,
               '' AS photo_path,
               latitude, longitude, notes, recorded_by, verification_status,
               verification_note, customer_name, customer_address, isSynced, lastSyncTime
        FROM meter_records
        WHERE recorded_by = :userId
        ORDER BY record_date DESC
        LIMIT 500
    """)
    fun getRecordsByUserId(userId: String): Flow<List<MeterRecordEntity>>

    @Query("""
        SELECT record_id, customer_id, meter_number, previous_reading, current_reading,
               usage_kwh, record_date, record_time, visit_status,
               '' AS photo_path,
               latitude, longitude, notes, recorded_by, verification_status,
               verification_note, customer_name, customer_address, isSynced, lastSyncTime
        FROM meter_records
        WHERE verification_status = :status
        ORDER BY record_date DESC
        LIMIT 500
    """)
    fun getRecordsByStatus(status: String): Flow<List<MeterRecordEntity>>

    @Query("""
        SELECT record_id, customer_id, meter_number, previous_reading, current_reading,
               usage_kwh, record_date, record_time, visit_status,
               '' AS photo_path,
               latitude, longitude, notes, recorded_by, verification_status,
               verification_note, customer_name, customer_address, isSynced, lastSyncTime
        FROM meter_records
        WHERE verification_status = :status AND recorded_by = :userId
        ORDER BY record_date DESC
        LIMIT 500
    """)
    fun getRecordsByStatusAndUser(status: String, userId: String): Flow<List<MeterRecordEntity>>

    @Query("""
        SELECT record_id, customer_id, meter_number, previous_reading, current_reading,
               usage_kwh, record_date, record_time, visit_status,
               '' AS photo_path,
               latitude, longitude, notes, recorded_by, verification_status,
               verification_note, customer_name, customer_address, isSynced, lastSyncTime
        FROM meter_records
        WHERE record_date = :date
        ORDER BY record_time DESC
        LIMIT 500
    """)
    fun getRecordsByDate(date: String): Flow<List<MeterRecordEntity>>

    @Query("SELECT * FROM meter_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<MeterRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MeterRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MeterRecordEntity>)

    @Update
    suspend fun update(record: MeterRecordEntity)

    @Query("UPDATE meter_records SET isSynced = 1 WHERE record_id = :recordId")
    suspend fun markAsSynced(recordId: String)

    @Query("UPDATE meter_records SET verification_status = :status, verification_note = :note WHERE record_id = :recordId")
    suspend fun updateVerificationStatus(recordId: String, status: String, note: String? = null)

    @Query("DELETE FROM meter_records")
    suspend fun deleteAll()

    @Query("DELETE FROM meter_records WHERE isSynced = 1")
    suspend fun deleteAllSynced()

    @Query("DELETE FROM meter_records WHERE isSynced = 1 AND recorded_by = :userId")
    suspend fun deleteSyncedByUser(userId: String)

    @Query("DELETE FROM meter_records WHERE isSynced = 1 AND recorded_by = :userId AND verification_status = :status")
    suspend fun deleteSyncedByUserAndStatus(userId: String, status: String)

    @Query("DELETE FROM meter_records WHERE isSynced = 1 AND verification_status = :status")
    suspend fun deleteSyncedByStatus(status: String)

    // Query DELETE tanpa filter isSynced - untuk replace total saat sync dari server
    @Query("DELETE FROM meter_records WHERE recorded_by = :userId AND verification_status = :status")
    suspend fun deleteAllByUserAndStatus(userId: String, status: String)

    @Query("DELETE FROM meter_records WHERE recorded_by = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("DELETE FROM meter_records WHERE verification_status = :status")
    suspend fun deleteAllByStatus(status: String)

    @Query("SELECT COUNT(*) FROM meter_records")
    suspend fun getCount(): Int
}
