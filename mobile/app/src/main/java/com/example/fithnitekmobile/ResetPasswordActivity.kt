package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityResetPasswordBinding
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.AuthViewModel

class ResetPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: AuthViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Autorisation envoyée par Ktor après vérification du code OTP
        val resetTicket = intent.getStringExtra("resetTicket")
        if (resetTicket.isNullOrBlank()) {
            toast("Vérifiez d'abord votre code", long = true)
            finish()
            return
        }

        binding.btnResetPassword.setOnClickListener {
            viewModel.resetPassword(
                ticket = resetTicket,
                password = binding.etNewPassword.text.toString(),
                confirmation = binding.etConfirmPassword.text.toString()
            )
        }

        viewModel.formError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                when (error.field) {
                    FormField.PASSWORD -> binding.etNewPassword.error = error.message
                    FormField.CONFIRM -> binding.etConfirmPassword.error = error.message
                    else -> toast(error.message)
                }
            }
        }

        viewModel.resetResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.btnResetPassword.isEnabled = false

                is Resource.Success -> {
                    binding.btnResetPassword.isEnabled = true
                    toast("Mot de passe modifié avec succès", long = true)
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }

                is Resource.Error -> {
                    binding.btnResetPassword.isEnabled = true
                    toast(state.message, long = true)
                }

                null -> Unit
            }
        }
    }
}
