package com.example.fithnitekmobile.di

import android.content.Context
import com.example.fithnitekmobile.network.RetrofitClient
import com.example.fithnitekmobile.repository.AuthRepository
import com.example.fithnitekmobile.repository.ConducteurRepository
import com.example.fithnitekmobile.repository.ProfilRepository
import com.example.fithnitekmobile.repository.ReservationRepository
import com.example.fithnitekmobile.repository.TrajetRepository
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.viewmodel.ViewModelFactory

// Crée UNE SEULE FOIS les objets partagés de l'application (session, API, repositories).
class AppContainer(context: Context) {

    val sessionManager = SessionManager(context)

    private val api = RetrofitClient.create(sessionManager)

    // Un repository par domaine
    val authRepository = AuthRepository(api, sessionManager)
    val trajetRepository = TrajetRepository(api)
    val reservationRepository = ReservationRepository(api)
    val conducteurRepository = ConducteurRepository(api, sessionManager)
    val profilRepository = ProfilRepository(api)

    val viewModelFactory = ViewModelFactory(this)
}