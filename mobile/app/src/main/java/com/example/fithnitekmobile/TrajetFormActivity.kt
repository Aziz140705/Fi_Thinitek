package com.example.fithnitekmobile

import android.R
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityTrajetFormBinding
import com.example.fithnitekmobile.util.CityCoordinates
import com.example.fithnitekmobile.util.GeoPoint
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.TrajetViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrajetFormActivity : BaseActivity() {

    private lateinit var binding: ActivityTrajetFormBinding
    private val viewModel: TrajetViewModel by viewModels { viewModelFactory }

    private val selectedCalendar = Calendar.getInstance()
    private var isDateSelected = false

    private val tunisianCities = arrayOf(
        "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul", "Hammamet", "Zaghouan",
        "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana", "Sousse", "Monastir",
        "Mahdia", "Sfax", "Kairouan", "Kasserine", "Sidi Bouzid", "Gabès",
        "Medenine", "Djerba", "Tataouine", "Gafsa", "Tozeur", "Kebili"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrajetFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }

        // Autocomplétion pour les villes tunisiennes
        val cityAdapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, tunisianCities)
        binding.etDepart.setAdapter(cityAdapter)
        binding.etDestination.setAdapter(cityAdapter)

        // Clic sur "Choisir la date et l'heure" -> Ouvre DatePicker puis TimePicker
        binding.btnDateHeure.setOnClickListener {
            showDatePicker()
        }

        binding.btnEnregistrer.setOnClickListener {
            val depart = binding.etDepart.text.toString().trim()
            val destination = binding.etDestination.text.toString().trim()
            val prixText = binding.etPrix.text.toString().trim()
            val placesText = binding.etPlaces.text.toString().trim()

            if (depart.isBlank()) {
                binding.etDepart.error = "Veuillez saisir le lieu de départ"
                return@setOnClickListener
            }
            if (destination.isBlank()) {
                binding.etDestination.error = "Veuillez saisir la destination"
                return@setOnClickListener
            }
            if (!isDateSelected) {
                binding.tvDateErreur.text = "Veuillez choisir la date et l'heure"
                binding.tvDateErreur.visibility = View.VISIBLE
                return@setOnClickListener
            }
            binding.tvDateErreur.visibility = View.GONE

            val prix = prixText.toDoubleOrNull()
            if (prix == null || prix <= 0) {
                binding.etPrix.error = "Veuillez entrer un prix valide"
                return@setOnClickListener
            }

            val places = placesText.toIntOrNull()
            if (places == null || places !in 1..8) {
                binding.etPlaces.error = "Veuillez entrer entre 1 et 8 places"
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val dateStr = dateFormat.format(selectedCalendar.time)
            val timeStr = timeFormat.format(selectedCalendar.time)

            val departGeo = CityCoordinates.get(depart) ?: GeoPoint(36.8065, 10.1815)
            val destGeo = CityCoordinates.get(destination) ?: GeoPoint(35.8256, 10.6369)

            viewModel.creerTrajet(
                depart = depart,
                destination = destination,
                dateDepart = dateStr,
                heureDepart = timeStr,
                prix = prix,
                places = places,
                departLat = departGeo.lat,
                departLng = departGeo.lng,
                destLat = destGeo.lat,
                destLng = destGeo.lng
            )
        }

        observeViewModel()
    }

    private fun showDatePicker() {
        val now = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = now.timeInMillis
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                isDateSelected = true

                val displayFormat = SimpleDateFormat("EEE d MMM yyyy 'à' HH:mm", Locale.FRENCH)
                binding.btnDateHeure.text = "📅 " + displayFormat.format(selectedCalendar.time)
                binding.tvDateErreur.visibility = View.GONE
            },
            selectedCalendar.get(Calendar.HOUR_OF_DAY),
            selectedCalendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun observeViewModel() {
        viewModel.creerResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> binding.btnEnregistrer.isEnabled = false
                is Resource.Success -> {
                    binding.btnEnregistrer.isEnabled = true
                    toast("Trajet publié avec succès !", long = true)
                    finish()
                }
                is Resource.Error -> {
                    binding.btnEnregistrer.isEnabled = true
                    showError(state.message)
                }
                null -> Unit
            }
        }
    }
}
