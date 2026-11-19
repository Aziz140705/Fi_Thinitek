package com.example.fithnitekmobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.fithnitekmobile.databinding.ActivityRechercheBinding

class RechercheActivity : BaseActivity() {

    companion object {
        fun newIntent(context: Context, args: Bundle): Intent {
            return Intent(context, RechercheActivity::class.java).apply {
                putExtras(args)
            }
        }
    }

    private lateinit var binding: ActivityRechercheBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRechercheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }
    }
}
