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
    @Query("SELECT * FROM meter_records ORDER BY record_date DESC, record_time DESC")
    fun getAllRecords(): Flow<List<MeterRecordEntity>>

    @Query("SELECT * FROM meter_records WHERE customer_id = :customerId ORDER BY record_date DESC")
    fun getRecordsByCustomerId(customerId: String): Flow<List<MeterRecordEntity>>

    @Query("SELECT * FROM meter_records WHERE recorded_by = :userId ORDER BY record_date DESC")
    fun getRecordsByUserId(userId: String): Flow<List<MeterRecordEntity>>

    @Query("SELECT * FROM meter_records WHERE verification_status = :status ORDER BY record_date DESC")
    fun getRecordsByStatus(status: String): Flow<List<MeterRecordEntity>>

    @Query("SELECT * FROM meter_records WHERE verification_status = :status AND recorded_by = :userId ORDER BY record_date DESC")
    fun getRecordsByStatusAndUser(status: String, userId: String): Flow<List<MeterRecordEntity>>

    @Query("SELECT * FROM meter_records WHERE record_date = :date ORDER BY record_time DESC")
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

    @Query("DELETE FROM meter_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM meter_records")
    suspend fun getCount(): Int
}
