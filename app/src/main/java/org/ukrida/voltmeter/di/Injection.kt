package org.ukrida.voltmeter.di

import android.content.Context
import org.ukrida.voltmeter.data.api.ApiService
import org.ukrida.voltmeter.data.api.RetrofitInstance
import org.ukrida.voltmeter.data.repository.LocalRepository
import org.ukrida.voltmeter.data.repository.VoltMeterRepository

object Injection {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val api: ApiService by lazy {
        try {
            RetrofitInstance.api
        } catch (e: Exception) {
            throw e
        }
    }

    val voltMeterRepo: VoltMeterRepository by lazy {
        VoltMeterRepository(api)
    }

    val localRepo: LocalRepository by lazy {
        LocalRepository(appContext)
    }
}
