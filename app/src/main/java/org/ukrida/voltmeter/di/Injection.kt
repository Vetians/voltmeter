package org.ukrida.voltmeter.di

import org.ukrida.voltmeter.data.api.ApiService
import org.ukrida.voltmeter.data.api.RetrofitInstance
import org.ukrida.voltmeter.data.repository.VoltMeterRepository

object Injection {

    private val api: ApiService = RetrofitInstance.api

    val voltMeterRepo: VoltMeterRepository by lazy {
        VoltMeterRepository(api)
    }
}
