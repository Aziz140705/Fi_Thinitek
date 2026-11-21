package com.example.fithnitekmobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.TrajetDto
import com.example.fithnitekmobile.models.TrajetRequest
import com.example.fithnitekmobile.models.TrajetResponse
import com.example.fithnitekmobile.repository.TrajetRepository
import com.example.fithnitekmobile.util.Event
import com.example.fithnitekmobile.util.Resource
import kotlinx.coroutines.launch

class TrajetViewModel(private val repository: TrajetRepository) : ViewModel() {

    private val _creerResult = MutableLiveData<Event<Resource<TrajetResponse>>>()
    val creerResult: LiveData<Event<Resource<TrajetResponse>>> = _creerResult

    private val _mesTrajetsResult = MutableLiveData<Resource<List<TrajetDto>>>()
    val mesTrajetsResult: LiveData<Resource<List<TrajetDto>>> = _mesTrajetsResult

    private val _supprimerResult = MutableLiveData<Event<Resource<Unit>>>()
    val supprimerResult: LiveData<Event<Resource<Unit>>> = _supprimerResult

    fun loadMesTrajets() {
        viewModelScope.launch {
            _mesTrajetsResult.value = Resource.Loading
            val res = repository.mesTrajets()
            if (res is Resource.Success) {
                _mesTrajetsResult.value = Resource.Success(res.data.trajets)
            } else if (res is Resource.Error) {
                _mesTrajetsResult.value = Resource.Error(res.message, res.code)
            }
        }
    }

    fun supprimerTrajet(idTrajet: Int) {
        viewModelScope.launch {
            _supprimerResult.value = Event(Resource.Loading)
            val res = repository.supprimerTrajet(idTrajet)
            if (res is Resource.Success) {
                _supprimerResult.value = Event(Resource.Success(Unit))
                loadMesTrajets()
            } else if (res is Resource.Error) {
                _supprimerResult.value = Event(Resource.Error(res.message, res.code))
            }
        }
    }

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
