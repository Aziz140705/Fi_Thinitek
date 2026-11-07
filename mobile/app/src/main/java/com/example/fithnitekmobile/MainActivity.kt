package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.fithnitekmobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var topAnim: Animation
    private lateinit var bottomAnim: Animation
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Chargement des animations pour la séquence en cascade (Staggered Animation)
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val titleAnim = AnimationUtils.loadAnimation(this, R.anim.title_animation)
        val arabicAnim = AnimationUtils.loadAnimation(this, R.anim.arabic_animation)
        val sloganAnim = AnimationUtils.loadAnimation(this, R.anim.slogan_animation)
        val btn1Anim = AnimationUtils.loadAnimation(this, R.anim.btn1_animation)
        val btn2Anim = AnimationUtils.loadAnimation(this, R.anim.btn2_animation)

        topAnim = logoAnim
        bottomAnim = btn1Anim

        // Déclenchement de la séquence d'animation progressive
        binding.imageView.startAnimation(logoAnim)
        binding.tvTitle.startAnimation(titleAnim)
        binding.tvArabic.startAnimation(arabicAnim)
        binding.tvSlogan.startAnimation(sloganAnim)
        binding.btnConnexion.startAnimation(btn1Anim)
        binding.btnInscription.startAnimation(btn2Anim)

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
