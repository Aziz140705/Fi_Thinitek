package com.example.fithnitekmobile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fithnitekmobile.databinding.ActivityRechercheBinding
import com.example.fithnitekmobile.models.TrajetDto
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.PassagerViewModel
import com.example.fithnitekmobile.viewmodel.ReservationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RechercheActivity : BaseActivity() {

    companion object {
        fun newIntent(context: Context, args: Bundle): Intent {
            return Intent(context, RechercheActivity::class.java).apply {
                putExtras(args)
            }
        }
    }

    private lateinit var binding: ActivityRechercheBinding
    private val viewModel: PassagerViewModel by viewModels { viewModelFactory }
    private val reservationViewModel: ReservationViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: TrajetAdapter

    private var selectedDateFormatted: String? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRechercheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val args = intent.extras
        val depart = args?.getString("depart").orEmpty()
        val destination = args?.getString("destination").orEmpty()

        if (depart.isNotBlank() || destination.isNotBlank()) {
            binding.tvCriteres.text = "$depart → $destination"
        } else {
            binding.tvCriteres.text = "Tous les trajets"
        }

        binding.btnRetour.setOnClickListener { finish() }

        binding.btnDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnEffacerDate.setOnClickListener {
            selectedDateFormatted = null
            binding.btnDate.text = "Toutes les dates"
            binding.btnEffacerDate.visibility = View.GONE
        }

        binding.btnAppliquer.setOnClickListener {
            viewModel.loadTrajetsDisponibles()
            toast("Filtres appliqués")
        }

        setupRecycler()
        observeViewModel()
        viewModel.loadTrajetsDisponibles()
    }

    private fun setupRecycler() {
        adapter = TrajetAdapter(
            onDeleteClick = null,
            onReserveClick = { trajet -> askPlaces(trajet) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
    }

    // Le passager choisit le nombre de places, puis la demande est envoyée
    private fun askPlaces(trajet: TrajetDto) {
        val max = minOf(trajet.placesDisponibles, 8)
        if (max < 1) {
            toast("Trajet complet")
            return
        }
        val labels = Array(max) { i -> if (i == 0) "1 place" else "${i + 1} places" }

        AlertDialog.Builder(this)
            .setTitle("Réserver ${trajet.depart} → ${trajet.destination}")
            .setItems(labels) { _, which ->
                reservationViewModel.reserver(trajet.idTrajet, which + 1)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val formatFull = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatDisplay = SimpleDateFormat("EEE d MMM yyyy", Locale.FRENCH)

                selectedDateFormatted = formatFull.format(calendar.time)
                binding.btnDate.text = "📅 " + formatDisplay.format(calendar.time)
                binding.btnEffacerDate.visibility = View.VISIBLE
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun observeViewModel() {
        viewModel.trajets.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        reservationViewModel.action.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    toast(state.data, long = true)
                    viewModel.loadTrajetsDisponibles()   // les places restantes ont changé
                }
                is Resource.Error -> showError(state.message)
                null -> Unit
            }
        }
    }
}