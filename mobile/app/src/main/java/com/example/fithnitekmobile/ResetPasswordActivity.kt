package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityResetPasswordBinding
import com.example.fithnitekmobile.network.ResetPasswordRequest
import com.example.fithnitekmobile.network.ResetPasswordResponse
import com.example.fithnitekmobile.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Autorisation envoyée par Ktor après vérification OTP
        val resetTicket = intent.getStringExtra("resetTicket")

        if (resetTicket.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Vérifiez d'abord votre code",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Changer le mot de passe
        binding.btnResetPassword.setOnClickListener {

            val password = binding.etNewPassword.text.toString()
            val confirmation = binding.etConfirmPassword.text.toString()

            when {
                password.length < 8 ->
                    binding.etNewPassword.error = "8 caractères minimum"

                password.toByteArray(Charsets.UTF_8).size > 72 ->
                    binding.etNewPassword.error = "72 octets maximum"

                password != confirmation ->
                    binding.etConfirmPassword.error =
                        "Mots de passe différents"

                else -> {
                    binding.btnResetPassword.isEnabled = false

                    RetrofitClient.api.resetPassword(
                        ResetPasswordRequest(
                            resetTicket = resetTicket,
                            newPassword = password
                        )
                    ).enqueue(object : Callback<ResetPasswordResponse> {

                        override fun onResponse(
                            call: Call<ResetPasswordResponse>,
                            response: Response<ResetPasswordResponse>
                        ) {
                            if (isFinishing || isDestroyed) return

                            binding.btnResetPassword.isEnabled = true

                            if (response.isSuccessful &&
                                response.body()?.success == true
                            ) {
                                Toast.makeText(
                                    this@ResetPasswordActivity,
                                    "Mot de passe modifié avec succès",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Retour à Login
                                val loginIntent = Intent(
                                    this@ResetPasswordActivity,
                                    LoginActivity::class.java
                                )

                                loginIntent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                                startActivity(loginIntent)
                                finish()

                            } else {
                                Toast.makeText(
                                    this@ResetPasswordActivity,
                                    "Autorisation expirée. Redemandez un code.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<ResetPasswordResponse>,
                            t: Throwable
                        ) {
                            if (isFinishing || isDestroyed) return

                            binding.btnResetPassword.isEnabled = true

                            Toast.makeText(
                                this@ResetPasswordActivity,
                                "Connexion impossible avec le serveur",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
                }
            }
        }
    }
}
