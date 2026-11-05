package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityLoginBinding
import com.example.fithnitekmobile.network.LoginRequest
import com.example.fithnitekmobile.network.LoginResponse
import com.example.fithnitekmobile.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retour à l'accueil
        binding.btnRetourAccueil.setOnClickListener {
            finish()
        }

        // US04 : Ouvrir la page Mot de passe oublié
        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(
                this,
                ForgotPasswordActivity::class.java
            )
            startActivity(intent)
        }

        // US02 : Bouton connexion
        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            when {
                email.isBlank() ->
                    binding.etEmail.error = "Email obligatoire"

                password.isBlank() ->
                    binding.etPassword.error = "Mot de passe obligatoire"

                else -> loginUser(LoginRequest(email, password))
            }
        }
    }

    private fun loginUser(request: LoginRequest) {

        binding.btnLogin.isEnabled = false

        // Appel du backend Ktor
        RetrofitClient.api.login(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnLogin.isEnabled = true
                    val result = response.body()

                    if (response.isSuccessful &&
                        result?.success == true &&
                        !result.token.isNullOrBlank()
                    ) {

                        // Enregistrer la session utilisateur
                        val session = getSharedPreferences(
                            "FiThnitekSession",
                            MODE_PRIVATE
                        )

                        session.edit()
                            .putString("token", result.token)
                            .putInt("idUser", result.idUser ?: -1)
                            .putString("role", result.role)
                            .apply()

                        Toast.makeText(
                            this@LoginActivity,
                            "Connexion réussie !",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Redirection selon le rôle
                        if (result.role == "PASSAGER") {

                            val intent = Intent(
                                this@LoginActivity,
                                PassagerActivity::class.java
                            )

                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Espace ${result.role} bientôt disponible",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {
                        val message = when (response.code()) {
                            401 -> "Email ou mot de passe incorrect"
                            400 -> "Informations invalides"
                            else -> "Erreur serveur : ${response.code()}"
                        }

                        Toast.makeText(
                            this@LoginActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnLogin.isEnabled = true

                    Toast.makeText(
                        this@LoginActivity,
                        "Connexion impossible avec le serveur",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
