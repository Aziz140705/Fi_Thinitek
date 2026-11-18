package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityVerifyCodeBinding
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.AuthViewModel

class VerifyCodeActivity : BaseActivity() {

    private lateinit var binding: ActivityVerifyCodeBinding
    private val viewModel: AuthViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVerifyCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Email envoyé par ForgotPasswordActivity
        val email = intent.getStringExtra("email")
        if (email.isNullOrBlank()) {
            toast("Adresse email manquante", long = true)
            finish()
            return
        }

        binding.tvEmailInfo.text = "Entrez le code à 6 chiffres envoyé à $email"
        binding.tvRetour.setOnClickListener { finish() }

        binding.btnVerifier.setOnClickListener {
            viewModel.verifyCode(email, binding.etCode.text.toString())
        }

        viewModel.formError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                if (error.field == FormField.CODE) binding.etCode.error = error.message
                else toast(error.message)
            }
        }

        viewModel.verifyResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.btnVerifier.isEnabled = false

                is Resource.Success -> {
                    binding.btnVerifier.isEnabled = true
                    toast("Code vérifié avec succès")
                    // Le ticket n'est donné par Ktor qu'après validation de l'OTP
                    startActivity(Intent(this, ResetPasswordActivity::class.java).apply {
                        putExtra("resetTicket", state.data.resetTicket)
                    })
                    finish()
                }

                is Resource.Error -> {
                    binding.btnVerifier.isEnabled = true
                    toast(state.message, long = true)
                }

                null -> Unit
            }
        }
    }
}
