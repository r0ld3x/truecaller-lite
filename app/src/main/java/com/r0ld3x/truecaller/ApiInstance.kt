package com.r0ld3x.truecaller

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query


interface ApiService {
    @GET("api")
    suspend fun getUserInfo(@Query("q") phone: String): ResponseTypes
}


object RetrofitClient {
    private const val BASE_URL = "https://tc-api-4759f72c5e83.herokuapp.com"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
