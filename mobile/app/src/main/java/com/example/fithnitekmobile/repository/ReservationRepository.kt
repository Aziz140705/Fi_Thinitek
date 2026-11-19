package com.example.fithnitekmobile.repository

import com.example.fithnitekmobile.models.ReservationListResponse
import com.example.fithnitekmobile.models.ReservationRequest
import com.example.fithnitekmobile.models.ReservationResponse
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.util.Resource

class ReservationRepository(private val api: ApiService) : BaseRepository() {

    suspend fun mesReservations(): Resource<ReservationListResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.mesReservations() }

    suspend fun reservationsRecues(): Resource<ReservationListResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.reservationsRecues() }

    suspend fun creerReservation(req: ReservationRequest): Resource<ReservationResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.creerReservation(req) }

    suspend fun accepterReservation(id: Int): Resource<ReservationResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.accepterReservation(id) }

    suspend fun refuserReservation(id: Int): Resource<ReservationResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.refuserReservation(id) }

    suspend fun annulerReservation(id: Int): Resource<ReservationResponse> =
        safeApiCall(businessError = { if (it.success) null else it.message }) { api.annulerReservation(id) }
}
