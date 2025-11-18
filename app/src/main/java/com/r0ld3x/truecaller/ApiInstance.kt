package com.r0ld3x.truecaller

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query
import androidx.core.content.edit
import retrofit2.http.Headers

data class ApiResponse<T>(
    val success: Boolean,
    val data: T
)

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
    suspend fun getUserInfo(@Query("q") phone: String): ApiResponse<ResponseTypes>
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

    suspend fun getUserInfoCached(context: Context, phone: String): Pair<ResponseTypes?, String?> {
        init(context)

        if (!isInternetAvailable(context)) {
            // Try to return cached data if available
            val cachedJson = sharedPreferences.getString(phone, null)
            if (cachedJson != null) {
                return try {
                    val cachedResponse = gson.fromJson(cachedJson, ResponseTypes::class.java)
                    Pair(cachedResponse, "Internet is off, using cached data")
                } catch (e: Exception) {
                    Log.e("CallService", "Cache parse error: ${e.message}")
                    Pair(null, "Cached data corrupted")
                }
            }
            return Pair(null, "Internet is off and no cached data")
        }

        return try {
            val apiResponse = api.getUserInfo(phone)
            Log.d("CallService", "API Response getUserInfoCached: $apiResponse")

            if (apiResponse.success) {
                val responseJson = gson.toJson(apiResponse.data)
                sharedPreferences.edit { putString(phone, responseJson) }
                if (!apiResponse.data.number.isNullOrEmpty()) {
                    sharedPreferences.edit { putString(apiResponse.data.number, responseJson) }
                }
                Pair(apiResponse.data, null)
            } else {
                Pair(null, "API returned failure or empty data")
            }
        } catch (e: Exception) {
            Log.e("CallService", "API Error: ${e.message} or ${e.localizedMessage}")
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
