package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityVerifyCodeBinding
import com.example.fithnitekmobile.network.RetrofitClient
import com.example.fithnitekmobile.network.VerifyCodeRequest
import com.example.fithnitekmobile.network.VerifyCodeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVerifyCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupérer l'email depuis ForgotPasswordActivity
        val email = intent.getStringExtra("email")

        if (email.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Adresse email manquante",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        binding.tvEmailInfo.text =
            "Entrez le code à 6 chiffres envoyé à $email"

        // Retour à la page précédente
        binding.tvRetour.setOnClickListener {
            finish()
        }

        // Vérifier le code OTP auprès du backend Ktor
        binding.btnVerifier.setOnClickListener {

            val code = binding.etCode.text.toString().trim()

            if (!code.matches(Regex("^[0-9]{6}$"))) {
                binding.etCode.error = "Entrez les 6 chiffres"
                return@setOnClickListener
            }

            binding.btnVerifier.isEnabled = false

            RetrofitClient.api.verifyCode(
                VerifyCodeRequest(email, code)
            ).enqueue(object : Callback<VerifyCodeResponse> {

                override fun onResponse(
                    call: Call<VerifyCodeResponse>,
                    response: Response<VerifyCodeResponse>
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnVerifier.isEnabled = true
                    val result = response.body()

                    if (response.isSuccessful &&
                        result?.success == true &&
                        !result.resetTicket.isNullOrBlank()
                    ) {
                        Toast.makeText(
                            this@VerifyCodeActivity,
                            "Code vérifié avec succès",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Ouvrir le nouveau mot de passe
                        // uniquement après validation par Ktor
                        val intent = Intent(
                            this@VerifyCodeActivity,
                            ResetPasswordActivity::class.java
                        )

                        intent.putExtra(
                            "resetTicket",
                            result.resetTicket
                        )

                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(
                            this@VerifyCodeActivity,
                            "Code incorrect, expiré ou bloqué",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<VerifyCodeResponse>,
                    t: Throwable
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnVerifier.isEnabled = true

                    Toast.makeText(
                        this@VerifyCodeActivity,
                        "Connexion impossible avec le serveur",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }
}
