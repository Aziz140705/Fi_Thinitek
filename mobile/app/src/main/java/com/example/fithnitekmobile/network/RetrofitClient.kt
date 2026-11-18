package com.example.fithnitekmobile.network

<<<<<<< HEAD
import com.example.fithnitekmobile.BuildConfig
import com.example.fithnitekmobile.session.SessionManager
=======
import android.os.Build
>>>>>>> f2369400a0dd5e45896ab772b3539cf2ffb2f5f8
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

<<<<<<< HEAD
    // Adresse du backend : définie dans app/build.gradle.kts (BuildConfig.API_BASE_URL).
    // Pour la changer : mobile/local.properties -> API_BASE_URL=http://IP_DU_PC:8080/
    // Ne jamais utiliser 127.0.0.1 depuis un téléphone : ce serait le téléphone lui-même.

    // Ajoute automatiquement  Authorization: Bearer <token>  aux routes protégées
    private class AuthInterceptor(private val session: SessionManager) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request()
            val isAuthRoute = original.url.encodedPath.startsWith("/api/auth/")
            val token = session.getToken()
=======
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
>>>>>>> f2369400a0dd5e45896ab772b3539cf2ffb2f5f8

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
