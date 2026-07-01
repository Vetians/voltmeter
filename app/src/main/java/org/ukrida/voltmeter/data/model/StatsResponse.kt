package org.ukrida.voltmeter.data.model

data class StatsResponse(
    val total_pelanggan: Int = 0,
    val total_meteran: Int = 0,
    val total_kunjungan: Int = 0,
    val total_verified: Int = 0,
    val total_unverified: Int = 0
)
