
package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ouvrir la page d'inscription
        findViewById<Button>(R.id.btnInscription).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Connexion : à développer ensuite
        findViewById<Button>(R.id.btnConnexion).setOnClickListener {
            Toast.makeText(this, "Connexion bientôt disponible", Toast.LENGTH_SHORT).show()
        }
    }
}
