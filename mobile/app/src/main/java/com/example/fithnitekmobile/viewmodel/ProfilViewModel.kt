package com.example.fithnitekmobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.ProfilDto
import com.example.fithnitekmobile.repository.ProfilRepository
import com.example.fithnitekmobile.util.Event
import com.example.fithnitekmobile.util.FormError
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import kotlinx.coroutines.launch

class ProfilViewModel(private val repository: ProfilRepository) : ViewModel() {

    private val _formError = MutableLiveData<Event<FormError>>()
    val formError: LiveData<Event<FormError>> = _formError

    private fun invalid(field: FormField, message: String) {
        _formError.value = Event(FormError(field, message))
    }

    private val _profil = MutableLiveData<Resource<ProfilDto>>()
    val profil: LiveData<Resource<ProfilDto>> = _profil

    fun load() {
        viewModelScope.launch {
            _profil.value = Resource.Loading
            _profil.value = repository.profil()
        }
    }

    private val _saveResult = MutableLiveData<Event<Resource<ProfilDto>>>()
    val saveResult: LiveData<Event<Resource<ProfilDto>>> = _saveResult

    fun enregistrer(nom: String, prenom: String, telephone: String) {
        val n = nom.trim()
        val p = prenom.trim()
        val t = telephone.trim()

        if (n.length < 2) return invalid(FormField.NOM, "Nom : 2 caractères minimum")
        if (p.length < 2) return invalid(FormField.PRENOM, "Prénom : 2 caractères minimum")
        if (t.isNotEmpty() && !t.matches(Regex("^\\+?[0-9 ]{8,15}$"))) {
            return invalid(FormField.TELEPHONE, "Téléphone invalide (8 à 15 chiffres)")
        }

        viewModelScope.launch {
            _saveResult.value = Event(Resource.Loading)
            _saveResult.value = Event(repository.modifier(n, p, t.ifEmpty { null }))
        }
    }
}
