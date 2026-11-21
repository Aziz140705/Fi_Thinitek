package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.models.TrajetListResponse
import com.example.fithnitekmobile.models.TrajetRequest
import com.example.fithnitekmobile.models.TrajetResponse
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.util.map

class TrajetRepository(private val api: ApiService) : BaseRepository() {

    suspend fun creerTrajet(req: TrajetRequest): Resource<TrajetResponse> =
        safeApiCall(businessError = { if (it.success && it.trajet != null) null else it.message }) {
            api.creerTrajet(req)
        }

    suspend fun mesTrajets(): Resource<TrajetListResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.mesTrajets() }

    suspend fun rechercherTrajets(
        depart: String? = null,
        destination: String? = null,
        date: String? = null,
        prixMax: Double? = null,
        placesMin: Int? = null,
        departLat: Double? = null,
        departLng: Double? = null,
        destLat: Double? = null,
        destLng: Double? = null,
        rayonKm: Double? = null
    ): Resource<TrajetListResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) {
            api.rechercherTrajets(depart, destination, date, prixMax, placesMin,
                departLat, departLng, destLat, destLng, rayonKm)
        }

    suspend fun supprimerTrajet(idTrajet: Int): Resource<Unit> =
        safeApiCall(businessError = { if (it.success) null else it.message }) {
            api.supprimerTrajet(idTrajet)
        }.map { }
}
