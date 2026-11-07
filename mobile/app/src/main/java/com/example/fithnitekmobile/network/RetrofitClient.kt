package com.example.fithnitekmobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 10.0.2.2 = localhost du PC depuis l'émulateur Android
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instance utilisée par les Activities
    val api: ApiService = retrofit.create(ApiService::class.java)
}
