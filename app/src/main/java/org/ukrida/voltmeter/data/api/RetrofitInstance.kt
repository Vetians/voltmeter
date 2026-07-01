package org.ukrida.voltmeter.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // private const val BASE_URL = "http://192.168.2.18/voltmeter_api/routes/"
    // private const val BASE_URL = "http://10.246.180.254/voltmeter_api/routes/"
     private const val BASE_URL = "http://10.0.2.2/voltmeter_api/routes/"

//    private const val BASE_URL = "http://10.246.180.254/voltmeter_api/routes/"
    const val IMAGE_BASE_URL = "http://10.0.2.2/voltmeter_api/uploads/photos/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
