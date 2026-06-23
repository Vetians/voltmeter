package org.ukrida.voltmeter.data.model

data class User(
    val id: Int = 0,
    val user_id: String = "",
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val role: String = "surveyor",
    val token: String = ""
)
