package com.example.fithnitekmobile.network

import com.example.fithnitekmobile.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------- Authentification ----------

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @POST("api/auth/verify-code")
    suspend fun verifyCode(@Body request: VerifyCodeRequest): Response<VerifyCodeResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResetPasswordResponse>

    // ---------- Profil (token JWT obligatoire) ----------

    @GET("api/users/me")
    suspend fun monProfil(): Response<ProfilResponse>

    @PUT("api/users/me")
    suspend fun modifierProfil(@Body request: ProfilUpdateRequest): Response<ProfilResponse>

    // ---------- Conducteur : dossier + paiement ----------

    @GET("api/conducteur/dossier")
    suspend fun monDossier(): Response<DossierResponse>

    @Multipart
    @POST("api/conducteur/dossier")
    suspend fun envoyerDossier(
        @Part("cin") cin: RequestBody,
        @Part("permis") permis: RequestBody,
        @Part("marque") marque: RequestBody,
        @Part("model") model: RequestBody,
        @Part("matricule") matricule: RequestBody,
        @Part("couleur") couleur: RequestBody,
        @Part("nbPlaces") nbPlaces: RequestBody,
        @Part("nbSiegesEnfant") nbSiegesEnfant: RequestBody,
        @Part cinPhoto: MultipartBody.Part,
        @Part permisPhoto: MultipartBody.Part,
        @Part vehiculePhoto: MultipartBody.Part
    ): Response<DossierResponse>

    @POST("api/conducteur/paiement")
    suspend fun payerConducteur(): Response<LoginResponse>

    @POST("api/users/devenir-conducteur")
    suspend fun devenirConducteur(): Response<LoginResponse>

    // ---------- Trajets (token JWT obligatoire) ----------

    @POST("api/trajets")
    suspend fun creerTrajet(@Body request: TrajetRequest): Response<TrajetResponse>

    @GET("api/trajets/mes")
    suspend fun mesTrajets(): Response<TrajetListResponse>

    @GET("api/trajets/search")
    suspend fun rechercherTrajets(
        @Query("depart") depart: String? = null,
        @Query("destination") destination: String? = null,
        @Query("date") date: String? = null,
        @Query("prixMax") prixMax: Double? = null,
        @Query("placesMin") placesMin: Int? = null,
        @Query("departLat") departLat: Double? = null,
        @Query("departLng") departLng: Double? = null,
        @Query("destLat") destLat: Double? = null,
        @Query("destLng") destLng: Double? = null,
        @Query("rayonKm") rayonKm: Double? = null
    ): Response<TrajetListResponse>

    @GET("api/trajets/{id}")
    suspend fun detailTrajet(@Path("id") id: Int): Response<TrajetResponse>

    @PUT("api/trajets/{id}")
    suspend fun modifierTrajet(
        @Path("id") id: Int,
        @Body request: TrajetRequest
    ): Response<TrajetResponse>

    @DELETE("api/trajets/{id}")
    suspend fun supprimerTrajet(@Path("id") id: Int): Response<MessageResponse>

    // ---------- Réservations (token JWT obligatoire) ----------

    @POST("api/reservations")
    suspend fun creerReservation(@Body request: ReservationRequest): Response<ReservationResponse>

    @GET("api/reservations/mes")
    suspend fun mesReservations(): Response<ReservationListResponse>

    @GET("api/reservations/recues")
    suspend fun reservationsRecues(): Response<ReservationListResponse>

    @PUT("api/reservations/{id}/accepter")
    suspend fun accepterReservation(@Path("id") id: Int): Response<ReservationResponse>

    @PUT("api/reservations/{id}/refuser")
    suspend fun refuserReservation(@Path("id") id: Int): Response<ReservationResponse>

    @PUT("api/reservations/{id}/annuler")
    suspend fun annulerReservation(@Path("id") id: Int): Response<ReservationResponse>
}
