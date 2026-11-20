package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.models.ProfilDto
import com.example.fithnitekmobile.models.ProfilResponse
import com.example.fithnitekmobile.models.ProfilUpdateRequest
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.util.map

class ProfilRepository(private val api: ApiService) : BaseRepository() {

    private val businessError: (ProfilResponse) -> String? =
        { if (it.success && it.profil != null) null else it.message }

    suspend fun profil(): Resource<ProfilDto> =
        safeApiCall(businessError = businessError) { api.monProfil() }.map { it.profil!! }

    suspend fun modifier(nom: String, prenom: String, telephone: String?): Resource<ProfilDto> =
        safeApiCall(businessError = businessError) {
            api.modifierProfil(ProfilUpdateRequest(nom, prenom, telephone))
        }.map { it.profil!! }
}
