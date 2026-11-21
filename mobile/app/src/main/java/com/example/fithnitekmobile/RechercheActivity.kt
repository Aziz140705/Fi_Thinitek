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
import com.example.fithnitekmobile.util.CityCoordinates
import com.example.fithnitekmobile.util.GeoPoint
import com.example.fithnitekmobile.util.GeoUtils
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
            applyFilter()
        }

        binding.btnAppliquer.setOnClickListener {
            applyFilter()
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
                applyFilter()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun applyFilter() {
        val rawList = viewModel.trajets.value.orEmpty()
        val filtered = filterTrips(rawList)
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterTrips(list: List<TrajetDto>): List<TrajetDto> {
        val args = intent.extras
        val searchDepart = args?.getString("depart").orEmpty().trim()
        val searchDest = args?.getString("destination").orEmpty().trim()

        val prixMax = binding.etPrixMax.text.toString().toDoubleOrNull()
        val placesMin = binding.etPlacesMin.text.toString().toIntOrNull()
        val rayonKm = binding.etRayon.text.toString().toDoubleOrNull() ?: 50.0

        val departGeo = args?.getDouble("departLat", 0.0)?.takeIf { it != 0.0 }?.let { lat ->
            val lng = args.getDouble("departLng", 0.0)
            GeoPoint(lat, lng)
        } ?: CityCoordinates.get(searchDepart)

        val destGeo = args?.getDouble("destLat", 0.0)?.takeIf { it != 0.0 }?.let { lat ->
            val lng = args.getDouble("destLng", 0.0)
            GeoPoint(lat, lng)
        } ?: CityCoordinates.get(searchDest)

        return list.filter { t ->
            val matchDepart = if (searchDepart.isBlank()) true else {
                val tGeo = CityCoordinates.get(t.depart) ?: (if (t.departLat != null && t.departLng != null) GeoPoint(t.departLat, t.departLng) else null)
                if (departGeo != null && tGeo != null) {
                    GeoUtils.distanceKm(departGeo.lat, departGeo.lng, tGeo.lat, tGeo.lng) <= rayonKm
                } else {
                    t.depart.contains(searchDepart, ignoreCase = true) || searchDepart.contains(t.depart, ignoreCase = true)
                }
            }

            val matchDest = if (searchDest.isBlank()) true else {
                val tGeo = CityCoordinates.get(t.destination) ?: (if (t.destLat != null && t.destLng != null) GeoPoint(t.destLat, t.destLng) else null)
                if (destGeo != null && tGeo != null) {
                    GeoUtils.distanceKm(destGeo.lat, destGeo.lng, tGeo.lat, tGeo.lng) <= rayonKm
                } else {
                    t.destination.contains(searchDest, ignoreCase = true) || searchDest.contains(t.destination, ignoreCase = true)
                }
            }

            val matchDate = if (selectedDateFormatted.isNullOrBlank()) true else {
                t.dateDepart == selectedDateFormatted
            }

            val matchPrix = if (prixMax == null) true else {
                t.prix <= prixMax
            }

            val matchPlaces = if (placesMin == null) true else {
                t.placesDisponibles >= placesMin
            }

            matchDepart && matchDest && matchDate && matchPrix && matchPlaces
        }
    }

    private fun observeViewModel() {
        viewModel.trajets.observe(this) { _ ->
            applyFilter()
        }

        reservationViewModel.action.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    toast(state.data, long = true)
                    viewModel.loadTrajetsDisponibles()
                }
                is Resource.Error -> showError(state.message)
                null -> Unit
            }
        }
    }
}
