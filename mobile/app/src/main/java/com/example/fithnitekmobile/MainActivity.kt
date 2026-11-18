package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityMainBinding
import com.example.fithnitekmobile.viewmodel.AuthViewModel

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    // ViewModel de session : MainActivity ne lit plus SharedPreferences elle-même
    private val authViewModel: AuthViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Déjà connecté (token valide) : on saute l'écran d'accueil
        if (authViewModel.authenticatedUser.value != null) {
            openHome()
            return
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Séquence d'animations en cascade
        binding.imageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.logo_animation))
        binding.tvTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.title_animation))
        binding.tvArabic.startAnimation(AnimationUtils.loadAnimation(this, R.anim.arabic_animation))
        binding.tvSlogan.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slogan_animation))
        binding.btnConnexion.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn1_animation))
        binding.btnInscription.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn2_animation))

        binding.btnInscription.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnConnexion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Si la session disparaît puis revient (ex : connexion dans un autre écran), on réagit
        authViewModel.authenticatedUser.observe(this) { user ->
            if (user != null) openHome()
        }
    }

    // Quand on revient sur l'accueil après une connexion / déconnexion, on relit la session
    override fun onResume() {
        super.onResume()
        authViewModel.refreshSession()
    }

    private fun openHome() {
        startActivity(Intent(this, PassagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
