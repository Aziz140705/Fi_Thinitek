package com.example.fithnitekmobile.network

import com.example.fithnitekmobile.BuildConfig
import com.example.fithnitekmobile.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Adresse du backend : définie dans app/build.gradle.kts (BuildConfig.API_BASE_URL).
    // Pour la changer : mobile/local.properties -> API_BASE_URL=http://IP_DU_PC:8080/
    // Ne jamais utiliser 127.0.0.1 depuis un téléphone : ce serait le téléphone lui-même.

    // Ajoute automatiquement  Authorization: Bearer <token>  aux routes protégées
    private class AuthInterceptor(private val session: SessionManager) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request()
            val isAuthRoute = original.url.encodedPath.startsWith("/api/auth/")
            val token = session.getToken()

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

    fun create(session: SessionManager): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(HttpLoggingInterceptor().apply {
                // BODY affiche aussi les mots de passe dans Logcat : à réserver au développement
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
