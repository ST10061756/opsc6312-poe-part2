package com.example.exploreo.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Place(
    val name: String?,
    val lon: Double?,
    val lat: Double?,
    val country: String?,
    val city: String?,
    val categories: List<String>?
)

data class PlacesResponse(val results: List<Place>?)

interface GeoapifyApi {
    @GET("v2/places")
    suspend fun getPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 20,
        @Query("apiKey") apiKey: String
    ): PlacesResponse
}

object GeoapifyClientProvider {
    private const val BASE_URL = "https://api.geoapify.com/"

    fun create(apiKey: String): GeoapifyApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GeoapifyApi::class.java)
    }
}


