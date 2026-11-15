package com.example.fieldshift

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // La URL oficial que Orejarena mandó al grupo
    private const val BASE_URL = "https://backfieldshift-production.up.railway.app/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Traduce el JSON a Kotlin
            .build()
            .create(ApiService::class.java)
    }
}