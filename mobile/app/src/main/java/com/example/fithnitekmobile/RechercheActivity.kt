package com.example.fithnitekmobile

import android.content.Context
import android.content.Intent
import android.os.Bundle

class RechercheActivity : BaseActivity() {

    companion object {
        fun newIntent(context: Context, args: Bundle): Intent {
            return Intent(context, RechercheActivity::class.java).apply {
                putExtras(args)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toast("Recherche de trajets")
    }
}
