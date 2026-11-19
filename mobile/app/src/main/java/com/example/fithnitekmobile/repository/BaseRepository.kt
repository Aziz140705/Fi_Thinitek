package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.util.Resource
import retrofit2.Response

abstract class BaseRepository {

    protected suspend fun <T> safeApiCall(
        businessError: ((T) -> String?)? = null,
        call: suspend () -> Response<T>
    ): Resource<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val err = businessError?.invoke(body)
                    if (err != null) {
                        Resource.Error(err, response.code())
                    } else {
                        Resource.Success(body)
                    }
                } else {
                    Resource.Error("Réponse vide du serveur", response.code())
                }
            } else {
                Resource.Error("Erreur ${response.code()} : ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Erreur réseau")
        }
    }
}
