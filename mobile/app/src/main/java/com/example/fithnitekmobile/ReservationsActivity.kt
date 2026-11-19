package com.example.fithnitekmobile

import android.os.Bundle
import com.example.fithnitekmobile.databinding.ActivityReservationsBinding

class ReservationsActivity : BaseActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }

    private lateinit var binding: ActivityReservationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }
    }
}
