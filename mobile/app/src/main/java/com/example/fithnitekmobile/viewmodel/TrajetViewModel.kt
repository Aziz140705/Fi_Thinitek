package com.example.fithnitekmobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.TrajetRequest
import com.example.fithnitekmobile.models.TrajetResponse
import com.example.fithnitekmobile.repository.TrajetRepository
import com.example.fithnitekmobile.util.Event
import com.example.fithnitekmobile.util.Resource
import kotlinx.coroutines.launch

class TrajetViewModel(private val repository: TrajetRepository) : ViewModel() {

    private val _creerResult = MutableLiveData<Event<Resource<TrajetResponse>>>()
    val creerResult: LiveData<Event<Resource<TrajetResponse>>> = _creerResult

    fun creerTrajet(
        depart: String,
        destination: String,
        dateDepart: String,
        heureDepart: String,
        prix: Double,
        places: Int,
        departLat: Double? = null,
        departLng: Double? = null,
        destLat: Double? = null,
        destLng: Double? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _creerResult.value = Event(Resource.Loading)
            val req = TrajetRequest(
                depart = depart,
                destination = destination,
                dateDepart = dateDepart,
                heureDepart = heureDepart,
                prix = prix,
                placesDisponibles = places,
                departLat = departLat,
                departLng = departLng,
                destLat = destLat,
                destLng = destLng,
                description = description
            )
            _creerResult.value = Event(repository.creerTrajet(req))
        }
    }
}
