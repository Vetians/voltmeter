package org.ukrida.voltmeter.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // Production - Hosting IFUKRIDA
    private const val BASE_URL = "https://voltmeter.ifukrida.net/voltmeter_api/routes/"
    const val IMAGE_BASE_URL = "https://voltmeter.ifukrida.net/voltmeter_api/uploads/photos/"

    // Local Development (uncomment untuk testing lokal)
    // private const val BASE_URL = "http://10.0.2.2/voltmeter_api/routes/"
    // const val IMAGE_BASE_URL = "http://10.0.2.2/voltmeter_api/uploads/photos/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
