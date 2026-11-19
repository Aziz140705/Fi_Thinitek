package com.example.fithnitekmobile

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.viewmodel.ViewModelFactory

abstract class BaseActivity : AppCompatActivity() {

    val sessionManager: SessionManager by lazy {
        SessionManager(applicationContext)
    }

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        ViewModelFactory(applicationContext)
    }

    fun toast(message: String, long: Boolean = false) {
        Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun showError(message: String) {
        toast(message, long = true)
    }

    fun goToWelcome() {
        sessionManager.clearSession()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
