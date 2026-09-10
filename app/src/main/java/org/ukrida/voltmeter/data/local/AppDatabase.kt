package org.ukrida.voltmeter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.ukrida.voltmeter.data.local.converter.MeterListConverter
import org.ukrida.voltmeter.data.local.dao.CustomerDao
import org.ukrida.voltmeter.data.local.dao.MeterRecordDao
import org.ukrida.voltmeter.data.local.dao.UserDao
import org.ukrida.voltmeter.data.local.entity.CustomerEntity
import org.ukrida.voltmeter.data.local.entity.MeterRecordEntity
import org.ukrida.voltmeter.data.local.entity.UserEntity

@Database(
    entities = [
        CustomerEntity::class,
        MeterRecordEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MeterListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun meterRecordDao(): MeterRecordDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voltmeter_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
