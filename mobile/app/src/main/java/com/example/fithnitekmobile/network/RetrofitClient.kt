package com.example.fithnitekmobile.network

import android.os.Build
import com.example.fithnitekmobile.BuildConfig
import com.example.fithnitekmobile.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

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
        get() = try {
            if (BuildConfig.API_BASE_URL.isNotBlank()) BuildConfig.API_BASE_URL
            else if (isEmulator) "http://10.0.2.2:8080/" else "http://192.168.1.16:8080/"
        } catch (e: Exception) {
            if (isEmulator) "http://10.0.2.2:8080/" else "http://192.168.1.16:8080/"
        }

    private class AuthInterceptor(private val session: SessionManager?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val isAuthRoute = original.url.encodedPath.startsWith("/api/auth/")
            val token = session?.getToken()

            val request = if (!isAuthRoute && !token.isNullOrBlank()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            return chain.proceed(request)
        }
    }

    private fun buildOkHttpClient(session: SessionManager? = null): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                try {
                    chain.proceed(request)
                } catch (e: Exception) {
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
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instance sans session (par défaut)
    val api: ApiService = retrofit.create(ApiService::class.java)

    // Instance avec session (pour les requêtes authentifiées)
    fun create(session: SessionManager): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttpClient(session))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}