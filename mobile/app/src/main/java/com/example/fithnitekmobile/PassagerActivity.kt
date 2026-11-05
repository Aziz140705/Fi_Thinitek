
package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityPassagerBinding
import org.json.JSONObject

class PassagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = getSharedPreferences(
            "FiThnitekSession", MODE_PRIVATE
        )

        val token = session.getString("token", null)
        val role = session.getString("role", null)

        // Vérification locale de la session
        if (token.isNullOrBlank() ||
            role != "PASSAGER" ||
            isTokenExpired(token)) {

            session.edit().clear().apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
            return
        }

        binding = ActivityPassagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvBienvenue.text = "Bienvenue, Passager !"

        binding.btnRechercher.setOnClickListener {
            Toast.makeText(
                this,
                "Recherche de trajets bientôt disponible",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnMesReservations.setOnClickListener {
            Toast.makeText(
                this,
                "Mes réservations bientôt disponibles",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnDeconnexion.setOnClickListener {
            session.edit().clear().apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }

    // Lire la date d'expiration contenue dans le JWT
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val decoded = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP
            )

            val payload = JSONObject(
                String(decoded, Charsets.UTF_8)
            )

            val exp = payload.getLong("exp")

            // exp est en secondes, Android utilise des millisecondes
            val currentTime = System.currentTimeMillis() / 1000L

            currentTime >= exp

        } catch (e: Exception) {
            true
        }
    }
}
