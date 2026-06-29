package org.ukrida.voltmeter.data.model

data class StatsResponse(
    val total_kunjungan: Int = 0,
    val terbaca_normal: Int = 0,
    val rumah_kosong: Int = 0,
    val halangan: Int = 0,
    val tidak_diterima: Int = 0
)
