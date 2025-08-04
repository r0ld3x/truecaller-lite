package com.r0ld3x.truecaller

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query
import androidx.core.content.edit
import retrofit2.http.Headers


data class ResponseTypes (
    val address: String,
    val birthday: String,
    val gender: String,
    val image: String,
    val name: String,
    val number: String?
)


interface ApiService {
    @GET("api")
    @Headers("Accept: application/json")
    suspend fun getUserInfo(@Query("q") phone: String): ResponseTypes
}


object RetrofitClient {
    private const val BASE_URL = "https://truecaller.underthedesk.blog"
    private const val PREF_NAME = "user_cache"


    private val gson = Gson()
    private lateinit var sharedPreferences: SharedPreferences


    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    suspend fun getUserInfoCached(context: Context,phone: String): Pair<ResponseTypes?, String?>{
         init(context)
        val cachedJson = sharedPreferences.getString(phone, null)
        if (cachedJson != null){
            return Pair(gson.fromJson(cachedJson, ResponseTypes::class.java), null)
        }
        if (!isInternetAvailable(context)) {
            return Pair(null, "Internet is off")
        }
        return try {
            val response = api.getUserInfo(phone)
            sharedPreferences.edit { putString(phone, gson.toJson(response)) }
            if (!response.number.isNullOrEmpty()) {
                sharedPreferences.edit { putString(response.number, gson.toJson(response)) }
            }
            Pair(response, null)
        } catch (e: Exception) {
            Pair(null, "Error getting user info: ${e.localizedMessage}")
        }
    }


    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }



}
