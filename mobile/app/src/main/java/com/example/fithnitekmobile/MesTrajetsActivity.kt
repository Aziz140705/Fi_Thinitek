package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fithnitekmobile.databinding.ActivityMesTrajetsBinding
import com.example.fithnitekmobile.models.ReservationMode
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.TrajetViewModel

class MesTrajetsActivity : BaseActivity() {

    private lateinit var binding: ActivityMesTrajetsBinding
    private val viewModel: TrajetViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: TrajetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMesTrajetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }

        binding.btnPublier.setOnClickListener {
            startActivity(Intent(this, TrajetFormActivity::class.java))
        }

        // Demandes reçues sur mes trajets : accepter ou refuser
        binding.btnDemandes.setOnClickListener {
            startActivity(Intent(this, ReservationsActivity::class.java).apply {
                putExtra(ReservationsActivity.EXTRA_MODE, ReservationMode.CONDUCTEUR.name)
            })
        }

        setupRecycler()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMesTrajets()
    }

    private fun setupRecycler() {
        adapter = TrajetAdapter(
            onDeleteClick = { trajet ->
                AlertDialog.Builder(this)
                    .setTitle("Annuler cette publication ?")
                    .setMessage("${trajet.depart} → ${trajet.destination}\n\nLes demandes en cours sur ce trajet seront annulées.")
                    .setPositiveButton("Oui, annuler") { _, _ -> viewModel.supprimerTrajet(trajet.idTrajet) }
                    .setNegativeButton("Non", null)
                    .show()
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.mesTrajetsResult.observe(this) { state ->
            when (state) {
                Resource.Loading -> binding.progress.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progress.visibility = View.GONE
                    adapter.submitList(state.data)
                    binding.tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.progress.visibility = View.GONE
                    showError(state.message)
                }
            }
        }

        viewModel.supprimerResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.progress.visibility = View.VISIBLE
                is Resource.Success -> toast("Publication annulée")
                is Resource.Error -> {
                    binding.progress.visibility = View.GONE
                    showError(state.message)
                }
                null -> Unit
            }
        }
    }
}