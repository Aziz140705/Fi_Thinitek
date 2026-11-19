package com.example.fithnitekmobile

import android.os.Bundle
import com.example.fithnitekmobile.databinding.ActivityTrajetFormBinding

class TrajetFormActivity : BaseActivity() {

    private lateinit var binding: ActivityTrajetFormBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrajetFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }
    }
}
