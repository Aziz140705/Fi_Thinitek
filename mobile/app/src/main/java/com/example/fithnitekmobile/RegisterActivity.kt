
package com.example.fithnitekmobile

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityRegisterBinding
import com.example.fithnitekmobile.network.RegisterRequest
import com.example.fithnitekmobile.network.RegisterResponse
import com.example.fithnitekmobile.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    // Accès aux composants de activity_register.xml
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation de ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retour à l'accueil
        binding.btnRetourAccueil.setOnClickListener {
            finish()
        }

        // Bouton inscription
        binding.btnRegister.setOnClickListener {

            // Récupérer les informations du formulaire
            val nom = binding.etNom.text.toString().trim()
            val prenom = binding.etPrenom.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val telephone = binding.etTelephone.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmation = binding.etConfirmPassword.text.toString()

            // Vérification des champs
            when {
                nom.isBlank() ->
                    binding.etNom.error = "Nom obligatoire"

                prenom.isBlank() ->
                    binding.etPrenom.error = "Prénom obligatoire"

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    binding.etEmail.error = "Email invalide"

                password.length < 8 ->
                    binding.etPassword.error = "8 caractères minimum"

                password.toByteArray(Charsets.UTF_8).size > 72 ->
                    binding.etPassword.error = "72 octets maximum"

                password != confirmation ->
                    binding.etConfirmPassword.error =
                        "Mots de passe différents"

                else -> {
                    // Préparer les données à envoyer au backend
                    val request = RegisterRequest(
                        nom = nom,
                        prenom = prenom,
                        email = email,
                        password = password,
                        telephone = telephone.ifBlank { null }
                    )

                    registerUser(request)
                }
            }
        }
    }

    // Envoyer la demande d'inscription à Ktor
    private fun registerUser(request: RegisterRequest) {

        binding.btnRegister.isEnabled = false

        RetrofitClient.api.register(request)
            .enqueue(object : Callback<RegisterResponse> {

                override fun onResponse(
                    call: Call<RegisterResponse>,
                    response: Response<RegisterResponse>
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnRegister.isEnabled = true

                    if (response.isSuccessful &&
                        response.body()?.success == true
                    ) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Compte créé avec succès !",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()

                    } else {
                        // Afficher le message selon le code HTTP
                        val message = when (response.code()) {
                            409 -> "Cet email est déjà utilisé"
                            400 -> "Informations invalides"
                            else -> "Erreur serveur : ${response.code()}"
                        }

                        Toast.makeText(
                            this@RegisterActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RegisterResponse>,
                    t: Throwable
                ) {
                    if (isFinishing || isDestroyed) return

                    binding.btnRegister.isEnabled = true

                    // Erreur réseau : serveur indisponible
                    Toast.makeText(
                        this@RegisterActivity,
                        "Connexion impossible avec le serveur",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
