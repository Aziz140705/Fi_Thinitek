package com.example.fithnitekmobile

import android.os.Bundle

class ReservationsActivity : BaseActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: "PASSAGER"
        toast("Mes Réservations ($mode)")
    }
}
