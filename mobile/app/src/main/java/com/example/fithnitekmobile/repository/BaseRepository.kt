package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.util.Resource
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

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
                Resource.Error(errorMessage(response), response.code())
            }
        } catch (e: IOException) {
            Resource.Error("Impossible de joindre le serveur. Vérifiez la connexion.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Erreur inattendue")
        }
    }

    // Lit le message envoyé par le serveur (ex : "Trajet complet"), sinon un message par défaut
    private fun errorMessage(response: Response<*>): String {
        val raw = try { response.errorBody()?.string() } catch (e: Exception) { null }
        val fromServer = try {
            if (raw.isNullOrBlank()) null else JSONObject(raw).optString("message").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
        return fromServer ?: when (response.code()) {
            401 -> "Session expirée, reconnectez-vous"
            403 -> "Action non autorisée"
            404 -> "Élément introuvable"
            409 -> "Action impossible dans l'état actuel"
            500 -> "Erreur du serveur"
            else -> "Erreur ${response.code()}"
        }
    }
}