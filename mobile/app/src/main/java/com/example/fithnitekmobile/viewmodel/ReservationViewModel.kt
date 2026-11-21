package com.example.fithnitekmobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.ReservationDto
import com.example.fithnitekmobile.models.ReservationMode
import com.example.fithnitekmobile.models.ReservationRequest
import com.example.fithnitekmobile.models.ReservationResponse
import com.example.fithnitekmobile.repository.ReservationRepository
import com.example.fithnitekmobile.util.Event
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.util.map
import kotlinx.coroutines.launch

class ReservationViewModel(private val repository: ReservationRepository) : ViewModel() {

    private val _liste = MutableLiveData<Resource<List<ReservationDto>>>()
    val liste: LiveData<Resource<List<ReservationDto>>> = _liste

    private val _action = MutableLiveData<Event<Resource<String>>>()
    val action: LiveData<Event<Resource<String>>> = _action

    fun load(mode: ReservationMode) {
        viewModelScope.launch {
            _liste.value = Resource.Loading
            val res = if (mode == ReservationMode.CONDUCTEUR) repository.reservationsRecues()
            else repository.mesReservations()
            _liste.value = res.map { it.reservations ?: emptyList() }
        }
    }

    fun reserver(idTrajet: Int, places: Int) =
        execute(null, "Demande envoyée ✅ Le conducteur doit l'accepter.") {
            repository.creerReservation(ReservationRequest(idTrajet, places))
        }

    fun accepter(id: Int, mode: ReservationMode) =
        execute(mode, "Réservation acceptée ✅") { repository.accepterReservation(id) }

    fun refuser(id: Int, mode: ReservationMode) =
        execute(mode, "Réservation refusée") { repository.refuserReservation(id) }

    fun annuler(id: Int, mode: ReservationMode) =
        execute(mode, "Réservation annulée") { repository.annulerReservation(id) }

    private fun execute(
        reloadMode: ReservationMode?,
        okMessage: String,
        call: suspend () -> Resource<ReservationResponse>
    ) {
        viewModelScope.launch {
            _action.value = Event(Resource.Loading)
            when (val res = call()) {
                is Resource.Success -> {
                    _action.value = Event(Resource.Success(okMessage))
                    if (reloadMode != null) load(reloadMode)
                }
                is Resource.Error -> _action.value = Event(Resource.Error(res.message, res.code))
                Resource.Loading -> Unit
            }
        }
    }
}
