package com.example.fieldshift

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // La URL oficial que Orejarena mandó al grupo
    private const val BASE_URL = "https://backfieldshift-production.up.railway.app/"

    // El análisis y la actividad consultan NASA POWER: los 10 s por defecto se quedan cortos
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        // El backend arma los textos en el idioma de la app ("es" o "en")
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Accept-Language", Idioma.actual()).build())
        }
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create()) // Traduce el JSON a Kotlin
            .build()
            .create(ApiService::class.java)
    }
}