package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.models.DossierDto
import com.example.fithnitekmobile.models.LoginResponse
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.util.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Données du formulaire (les photos sont déjà compressées en JPEG)
class DossierForm(
    val cin: String,
    val permis: String,
    val marque: String,
    val model: String,
    val matricule: String,
    val couleur: String,
    val nbPlaces: Int,
    val nbSiegesEnfant: Int,
    val cinPhoto: ByteArray,
    val permisPhoto: ByteArray,
    val vehiculePhoto: ByteArray
)

// Dossier conducteur + paiement des frais d'activation
class ConducteurRepository(
    private val api: ApiService,
    private val session: SessionManager
) : BaseRepository() {

    private fun text(value: String) = value.toRequestBody("text/plain; charset=utf-8".toMediaType())

    private fun photo(field: String, bytes: ByteArray) =
        MultipartBody.Part.createFormData(field, "$field.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))

    suspend fun dossier(): Resource<DossierDto> =
        safeApiCall(businessError = { if (it.success && it.dossier != null) null else it.message }) {
            api.monDossier()
        }.map { it.dossier!! }

    suspend fun envoyer(f: DossierForm): Resource<DossierDto> =
        safeApiCall(businessError = { if (it.success && it.dossier != null) null else it.message }) {
            api.envoyerDossier(
                cin = text(f.cin),
                permis = text(f.permis),
                marque = text(f.marque),
                model = text(f.model),
                matricule = text(f.matricule),
                couleur = text(f.couleur),
                nbPlaces = text(f.nbPlaces.toString()),
                nbSiegesEnfant = text(f.nbSiegesEnfant.toString()),
                cinPhoto = photo("cinPhoto", f.cinPhoto),
                permisPhoto = photo("permisPhoto", f.permisPhoto),
                vehiculePhoto = photo("vehiculePhoto", f.vehiculePhoto)
            )
        }.map { it.dossier!! }

    // Le serveur renvoie un NOUVEAU token (rôle CONDUCTEUR) : on le sauvegarde dans la session
    suspend fun payer(): Resource<LoginResponse> {
        val result = safeApiCall<LoginResponse>(
            businessError = { if (it.success && !it.token.isNullOrBlank()) null else it.message }
        ) { api.payerConducteur() }

        if (result is Resource.Success) {
            val token = result.data.token
            if (token != null) {
                session.saveSession(token, result.data.idUser ?: -1, result.data.role ?: "CONDUCTEUR")
            }
        }
        return result
    }
}
