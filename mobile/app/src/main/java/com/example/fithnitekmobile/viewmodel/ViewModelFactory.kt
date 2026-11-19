package com.example.fithnitekmobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fithnitekmobile.network.RetrofitClient
import com.example.fithnitekmobile.session.SessionManager

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val sessionManager = SessionManager(context)
        val apiService = RetrofitClient.create(sessionManager)

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(sessionManager, apiService) as T
            }
            modelClass.isAssignableFrom(PassagerViewModel::class.java) -> {
                PassagerViewModel(sessionManager, apiService) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
