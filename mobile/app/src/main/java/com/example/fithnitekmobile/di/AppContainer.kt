package com.example.fithnitekmobile.di

import android.content.Context
import com.example.fithnitekmobile.network.RetrofitClient
import com.example.fithnitekmobile.repository.AuthRepository
import com.example.fithnitekmobile.repository.ConducteurRepository
import com.example.fithnitekmobile.repository.ReservationRepository
import com.example.fithnitekmobile.repository.TrajetRepository
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.viewmodel.ViewModelFactory

class AppContainer(context: Context) {

    val sessionManager = SessionManager(context)

    private val api = RetrofitClient.create(sessionManager)

    val authRepository = AuthRepository(api, sessionManager)
    val trajetRepository = TrajetRepository(api)
    val reservationRepository = ReservationRepository(api)
    val conducteurRepository = ConducteurRepository(api, sessionManager)

    val viewModelFactory = ViewModelFactory(this)
}
