package com.example.fithnitekmobile.network

import android.os.Build
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Détection automatique :
    // - Émulateur Android (Pixel, etc.) -> 10.0.2.2
    // - Smartphone physique (Honor, etc.) -> 192.168.1.16
    private val isEmulator: Boolean
        get() = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)

    val BASE_URL: String
        get() = if (isEmulator) "http://10.0.2.2:8080/" else "http://192.168.1.16:8080/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(Interceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                // Basculement automatique si l'IP initiale échoue
                val urlString = request.url.toString()
                val fallbackUrlString = if (urlString.contains("10.0.2.2")) {
                    urlString.replace("10.0.2.2", "192.168.1.16")
                } else if (urlString.contains("192.168.1.16")) {
                    urlString.replace("192.168.1.16", "10.0.2.2")
                } else null

                if (fallbackUrlString != null) {
                    val newUrl = request.url.newBuilder(fallbackUrlString)?.build()
                    if (newUrl != null) {
                        val newRequest = request.newBuilder().url(newUrl).build()
                        return@Interceptor chain.proceed(newRequest)
                    }
                }
                throw e
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instance utilisée par les Activities
    val api: ApiService = retrofit.create(ApiService::class.java)
}