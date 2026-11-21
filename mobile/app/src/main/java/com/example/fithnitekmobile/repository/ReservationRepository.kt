package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.models.ReservationListResponse
import com.example.fithnitekmobile.models.ReservationRequest
import com.example.fithnitekmobile.models.ReservationResponse
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.util.Resource

class ReservationRepository(private val api: ApiService) : BaseRepository() {

    private val listError: (ReservationListResponse) -> String? =
        { if (it.success) null else it.message }

    private val singleError: (ReservationResponse) -> String? =
        { if (it.success && it.reservation != null) null else it.message }

    // Mes réservations (passager)
    suspend fun mesReservations(): Resource<ReservationListResponse> =
        safeApiCall(businessError = listError) { api.mesReservations() }

    // Demandes reçues sur mes trajets (conducteur)
    suspend fun reservationsRecues(): Resource<ReservationListResponse> =
        safeApiCall(businessError = listError) { api.reservationsRecues() }

    suspend fun creerReservation(req: ReservationRequest): Resource<ReservationResponse> =
        safeApiCall(businessError = singleError) { api.creerReservation(req) }

    suspend fun accepterReservation(id: Int): Resource<ReservationResponse> =
        safeApiCall(businessError = singleError) { api.accepterReservation(id) }

    suspend fun refuserReservation(id: Int): Resource<ReservationResponse> =
        safeApiCall(businessError = singleError) { api.refuserReservation(id) }

    suspend fun annulerReservation(id: Int): Resource<ReservationResponse> =
        safeApiCall(businessError = singleError) { api.annulerReservation(id) }
}
