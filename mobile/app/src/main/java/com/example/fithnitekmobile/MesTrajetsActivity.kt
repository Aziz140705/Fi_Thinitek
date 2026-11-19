package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import com.example.fithnitekmobile.databinding.ActivityMesTrajetsBinding
import com.example.fithnitekmobile.models.ReservationMode

class MesTrajetsActivity : BaseActivity() {

    private lateinit var binding: ActivityMesTrajetsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMesTrajetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }

        binding.btnPublier.setOnClickListener {
            startActivity(Intent(this, TrajetFormActivity::class.java))
        }

        binding.btnDemandes.setOnClickListener {
            startActivity(Intent(this, ReservationsActivity::class.java).apply {
                putExtra(ReservationsActivity.EXTRA_MODE, ReservationMode.CONDUCTEUR.name)
            })
        }
    }
}
