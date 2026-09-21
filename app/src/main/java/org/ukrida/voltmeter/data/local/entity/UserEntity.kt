package org.ukrida.voltmeter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val user_id: String,
    val name: String,
    val username: String,
    val password: String,
    val role: String,
    val token: String,
    val lastSyncTime: Long = System.currentTimeMillis()
)
