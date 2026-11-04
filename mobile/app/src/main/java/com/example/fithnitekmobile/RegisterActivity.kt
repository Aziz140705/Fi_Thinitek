
package com.example.fithnitekmobile

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nom = findViewById<EditText>(R.id.etNom)
        val prenom = findViewById<EditText>(R.id.etPrenom)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirm = findViewById<EditText>(R.id.etConfirmPassword)

        findViewById<Button>(R.id.btnRegister).setOnClickListener {

            val n = nom.text.toString().trim()
            val p = prenom.text.toString().trim()
            val e = email.text.toString().trim()
            val mdp = password.text.toString()

            when {
                n.isEmpty() -> nom.error = "Nom obligatoire"
                p.isEmpty() -> prenom.error = "Prénom obligatoire"
                !Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                    email.error = "Email invalide"

                mdp.length < 8 ->
                    password.error = "8 caractères minimum"

                mdp.toByteArray(Charsets.UTF_8).size > 72 ->
                    password.error = "72 octets maximum"

                mdp != confirm.text.toString() ->
                    confirm.error = "Mots de passe différents"

                else -> Toast.makeText(
                    this,
                    "Formulaire valide !",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
