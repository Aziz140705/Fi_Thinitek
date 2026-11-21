package com.example.fithnitekmobile

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fithnitekmobile.databinding.ActivityReservationsBinding
import com.example.fithnitekmobile.models.ReservationMode
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.ReservationViewModel

// PASSAGER : mes réservations (annulation possible).
// CONDUCTEUR : demandes reçues sur mes trajets (accepter / refuser).
class ReservationsActivity : BaseActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }

    private lateinit var binding: ActivityReservationsBinding
    private val viewModel: ReservationViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: ReservationAdapter
    private var mode = ReservationMode.PASSAGER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = try {
            ReservationMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: ReservationMode.PASSAGER.name)
        } catch (e: Exception) {
            ReservationMode.PASSAGER
        }

        binding.tvTitre.text =
            if (mode == ReservationMode.CONDUCTEUR) "Demandes reçues" else "Mes réservations"
        binding.tvEmpty.text =
            if (mode == ReservationMode.CONDUCTEUR) "Aucune demande reçue pour le moment"
            else "Aucune réservation pour le moment"

        binding.btnRetour.setOnClickListener { finish() }

        adapter = ReservationAdapter(
            mode = mode,
            onAccepter = { viewModel.accepter(it.idReservation, mode) },
            onRefuser = { r ->
                confirmer("Refuser cette demande ?", "Le passager sera prévenu et les places seront rendues.") {
                    viewModel.refuser(r.idReservation, mode)
                }
            },
            onAnnuler = { r ->
                confirmer("Annuler la réservation ?", "Les places seront rendues au conducteur.") {
                    viewModel.annuler(r.idReservation, mode)
                }
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(mode)
    }

    private fun confirmer(titre: String, message: String, onOk: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(titre)
            .setMessage(message)
            .setPositiveButton("Oui") { _, _ -> onOk() }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.liste.observe(this) { state ->
            when (state) {
                Resource.Loading -> binding.progress.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progress.visibility = View.GONE
                    adapter.submitList(state.data)
                    binding.tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.progress.visibility = View.GONE
                    if (state.code == 401) goToWelcome() else showError(state.message)
                }
            }
        }

        viewModel.action.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.progress.visibility = View.VISIBLE
                is Resource.Success -> toast(state.data, long = true)
                is Resource.Error -> {
                    binding.progress.visibility = View.GONE
                    showError(state.message)
                }
                null -> Unit
            }
        }
    }
}