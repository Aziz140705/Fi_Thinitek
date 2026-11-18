package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityForgotPasswordBinding
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.AuthViewModel

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvRetourLogin.setOnClickListener { finish() }

        binding.btnEnvoyer.setOnClickListener {
            viewModel.forgotPassword(binding.etEmail.text.toString())
        }

        viewModel.formError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                if (error.field == FormField.EMAIL) binding.etEmail.error = error.message
                else toast(error.message)
            }
        }

        viewModel.forgotResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.btnEnvoyer.isEnabled = false

                is Resource.Success -> {
                    binding.btnEnvoyer.isEnabled = true
                    toast("Si cet email existe, un code sera envoyé", long = true)
                    startActivity(Intent(this, VerifyCodeActivity::class.java).apply {
                        putExtra("email", binding.etEmail.text.toString().trim())
                    })
                }

                is Resource.Error -> {
                    binding.btnEnvoyer.isEnabled = true
                    toast(state.message, long = true)
                }

                null -> Unit
            }
        }
    }
}
