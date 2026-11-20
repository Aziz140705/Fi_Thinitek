package com.example.fithnitekmobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fithnitekmobile.di.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    constructor(context: Context) : this(AppContainer(context))

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(container.sessionManager, container.authRepository) as T

            modelClass.isAssignableFrom(TrajetViewModel::class.java) ->
                TrajetViewModel(container.trajetRepository) as T

            modelClass.isAssignableFrom(PassagerViewModel::class.java) ->
                PassagerViewModel(container.sessionManager, container.trajetRepository) as T

            modelClass.isAssignableFrom(ConducteurViewModel::class.java) ->
                ConducteurViewModel(container.conducteurRepository) as T

            modelClass.isAssignableFrom(ProfilViewModel::class.java) ->
                ProfilViewModel(container.profilRepository) as T

            else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
        }
    }
}
