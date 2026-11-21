package com.example.fithnitekmobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
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
                viewModel.supprimerTrajet(trajet.idTrajet)
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
                    if (state.data.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                    }
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
                is Resource.Success -> {
                    toast("Trajet supprimé avec succès")
                }
                is Resource.Error -> {
                    showError(state.message)
                }
                null -> Unit
            }
        }
    }
}
