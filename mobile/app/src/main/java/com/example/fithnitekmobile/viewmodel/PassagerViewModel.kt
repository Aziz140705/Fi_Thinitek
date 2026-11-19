package com.example.fithnitekmobile.viewmodel

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.TrajetDto
import com.example.fithnitekmobile.repository.TrajetRepository
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.util.GeoPoint
import com.example.fithnitekmobile.util.Resource
import kotlinx.coroutines.launch

class PassagerViewModel(
    private val sessionManager: SessionManager,
    private val repository: TrajetRepository
) : ViewModel() {

    private val _mapInfo = MutableLiveData<String>()
    val mapInfo: LiveData<String> = _mapInfo

    private val _trajets = MutableLiveData<List<TrajetDto>>(emptyList())
    val trajets: LiveData<List<TrajetDto>> = _trajets

    private val _passagerPosition = MutableLiveData<GeoPoint?>()
    val passagerPosition: LiveData<GeoPoint?> = _passagerPosition

    private val _depart = MutableLiveData<GeoPoint?>()
    val depart: LiveData<GeoPoint?> = _depart

    private val _destination = MutableLiveData<GeoPoint?>()
    val destination: LiveData<GeoPoint?> = _destination

    var departLabel: String? = "Position actuelle"
    var selectingDepart: Boolean = false

    fun setPassagerPosition(point: GeoPoint) {
        _passagerPosition.value = point
    }

    fun loadTrajetsDisponibles() {
        viewModelScope.launch {
            val res = repository.rechercherTrajets()
            if (res is Resource.Success) {
                _trajets.value = res.data.trajets
            }
        }
    }

    fun selectDestination() {
        selectingDepart = false
    }

    fun onMapTapped(point: GeoPoint): Boolean {
        if (selectingDepart) {
            _depart.value = point
        } else {
            _destination.value = point
        }
        return true
    }

    fun buildSearchParams(departText: String, destinationText: String): Bundle {
        return Bundle().apply {
            putString("depart", departText)
            putString("destination", destinationText)
            _depart.value?.let {
                putDouble("departLat", it.lat)
                putDouble("departLng", it.lng)
            }
            _destination.value?.let {
                putDouble("destLat", it.lat)
                putDouble("destLng", it.lng)
            }
        }
    }
}
