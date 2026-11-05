
package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ouvrir la page inscription
        binding.btnInscription.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Connexion : prochaine étape
        binding.btnConnexion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
