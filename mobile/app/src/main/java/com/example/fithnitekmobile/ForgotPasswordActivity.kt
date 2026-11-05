package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityForgotPasswordBinding
import com.example.fithnitekmobile.network.ForgotPasswordRequest
import com.example.fithnitekmobile.network.ForgotPasswordResponse
import com.example.fithnitekmobile.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retour à la connexion
        binding.tvRetourLogin.setOnClickListener {
            finish()
        }

        // Demander le code par Gmail
        binding.btnEnvoyer.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Adresse email invalide"
                return@setOnClickListener
            }

            binding.btnEnvoyer.isEnabled = false

            RetrofitClient.api.forgotPassword(
                ForgotPasswordRequest(email)
            ).enqueue(object : Callback<ForgotPasswordResponse> {

                override fun onResponse(
                    call: Call<ForgotPasswordResponse>,
                    response: Response<ForgotPasswordResponse>
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnEnvoyer.isEnabled = true

                    if (response.isSuccessful &&
                        response.body()?.success == true
                    ) {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Si cet email existe, un code sera envoyé",
                            Toast.LENGTH_LONG
                        ).show()

                        // Passer à l'écran OTP avec l'email
                        val intent = Intent(
                            this@ForgotPasswordActivity,
                            VerifyCodeActivity::class.java
                        )

                        intent.putExtra("email", email)
                        startActivity(intent)

                    } else {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Erreur lors de la demande",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<ForgotPasswordResponse>,
                    t: Throwable
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnEnvoyer.isEnabled = true

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Connexion impossible avec le serveur",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }
}
