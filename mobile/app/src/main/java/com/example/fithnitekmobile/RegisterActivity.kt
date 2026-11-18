package com.example.fithnitekmobile

import android.os.Bundle
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityRegisterBinding
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.AuthViewModel

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetourAccueil.setOnClickListener { finish() }

        // Toutes les vérifications (dont le mot de passe fort) sont dans le ViewModel
        binding.btnRegister.setOnClickListener {
            viewModel.register(
                nom = binding.etNom.text.toString(),
                prenom = binding.etPrenom.text.toString(),
                email = binding.etEmail.text.toString(),
                telephone = binding.etTelephone.text.toString(),
                password = binding.etPassword.text.toString(),
                confirmation = binding.etConfirmPassword.text.toString()
            )
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.formError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                when (error.field) {
                    FormField.NOM -> binding.etNom.error = error.message
                    FormField.PRENOM -> binding.etPrenom.error = error.message
                    FormField.EMAIL -> binding.etEmail.error = error.message
                    FormField.PASSWORD -> binding.etPassword.error = error.message
                    FormField.CONFIRM -> binding.etConfirmPassword.error = error.message
                    else -> toast(error.message)
                }
            }
        }

        viewModel.registerResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.btnRegister.isEnabled = false

                is Resource.Success -> {
                    binding.btnRegister.isEnabled = true
                    toast("Compte créé avec succès !", long = true)
                    finish()
                }

                is Resource.Error -> {
                    binding.btnRegister.isEnabled = true
                    val message = if (state.code == 409) "Cet email est déjà utilisé" else state.message
                    toast(message, long = true)
                }

                null -> Unit
            }
        }
    }
}
