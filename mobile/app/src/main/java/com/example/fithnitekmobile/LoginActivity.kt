package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityLoginBinding
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.AuthViewModel

// VIEW : affiche l'écran, envoie les actions au ViewModel et observe ses résultats.
class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetourAccueil.setOnClickListener { finish() }

        // US04 : mot de passe oublié
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // US02 : le ViewModel valide puis appelle le repository
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString()
            )
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.formError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                when (error.field) {
                    FormField.EMAIL -> binding.etEmail.error = error.message
                    FormField.PASSWORD -> binding.etPassword.error = error.message
                    else -> toast(error.message)
                }
            }
        }

        viewModel.loginResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.btnLogin.isEnabled = false

                is Resource.Success -> {
                    binding.btnLogin.isEnabled = true
                    toast("Connexion réussie !")
                    startActivity(Intent(this, PassagerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }

                is Resource.Error -> {
                    binding.btnLogin.isEnabled = true
                    // Ici 401 = mauvais identifiants (pas une session expirée) : simple Toast
                    toast(state.message, long = true)
                }

                null -> Unit // événement déjà consommé
            }
        }
    }
}
