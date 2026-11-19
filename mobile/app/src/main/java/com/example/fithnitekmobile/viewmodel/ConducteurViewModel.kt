package com.example.fithnitekmobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.DossierDto
import com.example.fithnitekmobile.models.LoginResponse
import com.example.fithnitekmobile.repository.ConducteurRepository
import com.example.fithnitekmobile.repository.DossierForm
import com.example.fithnitekmobile.util.Event
import com.example.fithnitekmobile.util.FormError
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import kotlinx.coroutines.launch

// ViewModel du dossier conducteur et du paiement
class ConducteurViewModel(private val repository: ConducteurRepository) : ViewModel() {

    // Photos choisies : gardées ici pour survivre à une rotation de l'écran
    var cinPhoto: ByteArray? = null
    var permisPhoto: ByteArray? = null
    var vehiculePhoto: ByteArray? = null

    private val _formError = MutableLiveData<Event<FormError>>()
    val formError: LiveData<Event<FormError>> = _formError

    private fun invalid(field: FormField, message: String) {
        _formError.value = Event(FormError(field, message))
    }

    // ---------- Lire mon dossier ----------
    private val _dossier = MutableLiveData<Resource<DossierDto>>()
    val dossier: LiveData<Resource<DossierDto>> = _dossier

    fun load() {
        viewModelScope.launch {
            _dossier.value = Resource.Loading
            _dossier.value = repository.dossier()
        }
    }

    // ---------- Envoyer le dossier (vérification automatique côté serveur) ----------
    private val _envoiResult = MutableLiveData<Event<Resource<DossierDto>>>()
    val envoiResult: LiveData<Event<Resource<DossierDto>>> = _envoiResult

    fun envoyer(
        cin: String, permis: String, marque: String, model: String, matricule: String,
        couleur: String, placesText: String, siegesText: String
    ) {
        val c = cin.trim()
        val p = permis.trim()
        val mq = marque.trim()
        val md = model.trim()
        val mat = matricule.trim()
        val places = placesText.trim().toIntOrNull()
        val sieges = siegesText.trim().ifEmpty { "0" }.toIntOrNull()

        when {
            !c.matches(Regex("^[0-9]{8}$")) -> return invalid(FormField.CIN, "CIN : 8 chiffres")
            p.length < 4 -> return invalid(FormField.PERMIS, "Numéro de permis invalide")
            mq.length < 2 -> return invalid(FormField.MARQUE, "Marque obligatoire")
            md.isEmpty() -> return invalid(FormField.MODELE, "Modèle obligatoire")
            mat.length < 4 -> return invalid(FormField.MATRICULE, "Immatriculation invalide")
            places == null || places < 1 || places > 8 -> return invalid(FormField.PLACES, "Entre 1 et 8 places")
            sieges == null || sieges < 0 || sieges > places -> return invalid(FormField.PLACES, "Sièges enfant invalides")
        }

        val cinBytes = cinPhoto ?: return invalid(FormField.PHOTOS, "Ajoutez la photo de votre CIN")
        val permisBytes = permisPhoto ?: return invalid(FormField.PHOTOS, "Ajoutez la photo de votre permis")
        val vehiculeBytes = vehiculePhoto ?: return invalid(FormField.PHOTOS, "Ajoutez la photo du véhicule")

        val form = DossierForm(
            cin = c, permis = p, marque = mq, model = md, matricule = mat,
            couleur = couleur.trim(), nbPlaces = places!!, nbSiegesEnfant = sieges!!,
            cinPhoto = cinBytes, permisPhoto = permisBytes, vehiculePhoto = vehiculeBytes
        )

        viewModelScope.launch {
            _envoiResult.value = Event(Resource.Loading)
            _envoiResult.value = Event(repository.envoyer(form))
        }
    }

    // ---------- Payer ----------
    private val _paiementResult = MutableLiveData<Event<Resource<LoginResponse>>>()
    val paiementResult: LiveData<Event<Resource<LoginResponse>>> = _paiementResult

    fun payer() {
        viewModelScope.launch {
            _paiementResult.value = Event(Resource.Loading)
            _paiementResult.value = Event(repository.payer())
        }
    }
}
