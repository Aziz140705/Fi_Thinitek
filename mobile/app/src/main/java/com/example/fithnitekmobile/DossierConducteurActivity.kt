package com.example.fithnitekmobile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.fithnitekmobile.databinding.ActivityDossierConducteurBinding
import com.example.fithnitekmobile.models.DossierDto
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.ImageUtils
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.ConducteurViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DossierConducteurActivity : BaseActivity() {

    private lateinit var binding: ActivityDossierConducteurBinding
    private val viewModel: ConducteurViewModel by viewModels { viewModelFactory }

    private var frais = 0.0

    private val pickCin = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadPhoto(it) { bytes -> viewModel.cinPhoto = bytes } }
    }
    private val pickPermis = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadPhoto(it) { bytes -> viewModel.permisPhoto = bytes } }
    }
    private val pickVehicule = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadPhoto(it) { bytes -> viewModel.vehiculePhoto = bytes } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDossierConducteurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }
        binding.btnPhotoCin.setOnClickListener { pickCin.launch("image/*") }
        binding.btnPhotoPermis.setOnClickListener { pickPermis.launch("image/*") }
        binding.btnPhotoVehicule.setOnClickListener { pickVehicule.launch("image/*") }
        binding.btnModifier.setOnClickListener { showForm() }
        binding.btnPayer.setOnClickListener { confirmPaiement() }

        binding.btnEnvoyer.setOnClickListener {
            clearErrors()
            viewModel.envoyer(
                cin = binding.etCin.text.toString(),
                permis = binding.etPermis.text.toString(),
                marque = binding.etMarque.text.toString(),
                model = binding.etModel.text.toString(),
                matricule = binding.etMatricule.text.toString(),
                couleur = binding.etCouleur.text.toString(),
                placesText = binding.etPlaces.text.toString(),
                siegesText = binding.etSieges.text.toString()
            )
        }

        renderPhotos()
        observeViewModel()
        viewModel.load()
    }

    private fun observeViewModel() {
        viewModel.dossier.observe(this) { state ->
            when (state) {
                Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    render(state.data)
                }
                is Resource.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
            }
        }

        viewModel.formError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                when (error.field) {
                    FormField.CIN -> binding.etCin.error = error.message
                    FormField.PERMIS -> binding.etPermis.error = error.message
                    FormField.MARQUE -> binding.etMarque.error = error.message
                    FormField.MODELE -> binding.etModel.error = error.message
                    FormField.MATRICULE -> binding.etMatricule.error = error.message
                    FormField.PLACES -> binding.etPlaces.error = error.message
                    FormField.PHOTOS -> {
                        binding.tvPhotoErreur.text = error.message
                        binding.tvPhotoErreur.visibility = View.VISIBLE
                    }
                    else -> toast(error.message)
                }
            }
        }

        viewModel.envoiResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    toast("Dossier vérifié ✅ Il ne reste que le paiement.", long = true)
                    render(state.data)
                }
                is Resource.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
                null -> Unit
            }
        }

        viewModel.paiementResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    toast("Paiement confirmé. Vous êtes maintenant conducteur 🚗", long = true)
                    startActivity(Intent(this, MesTrajetsActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
                null -> Unit
            }
        }
    }

    private fun render(d: DossierDto) {
        frais = d.frais
        binding.tvFrais.text = String.format(Locale.FRANCE, "Frais d'activation : %.2f DT", d.frais)

        when {
            d.statut == "VALIDATED" -> {
                startActivity(Intent(this, MesTrajetsActivity::class.java))
                finish()
            }

            d.existe && d.statut == "PENDING" -> {
                binding.tvResume.text =
                    "${d.marque} ${d.model} · ${d.matricule}\n" +
                            "${d.nbPlaces} place(s) · ${d.nbSiegesEnfant} siège(s) enfant"
                binding.cardPaiement.visibility = View.VISIBLE
                binding.formContainer.visibility = View.GONE
                prefill(d)
            }

            else -> {
                binding.cardPaiement.visibility = View.GONE
                binding.formContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun showForm() {
        binding.formContainer.visibility = View.VISIBLE
        binding.btnEnvoyer.text = "Mettre à jour mon dossier"
        toast("Ajoutez de nouveau les 3 photos pour modifier votre dossier.", long = true)
    }

    private fun prefill(d: DossierDto) {
        fun fill(edit: EditText, value: String?) {
            if (edit.text.isNullOrEmpty() && value != null) edit.setText(value)
        }
        fill(binding.etMarque, d.marque)
        fill(binding.etModel, d.model)
        fill(binding.etMatricule, d.matricule)
        fill(binding.etCouleur, d.couleur)
        fill(binding.etPlaces, d.nbPlaces?.toString())
        fill(binding.etSieges, d.nbSiegesEnfant?.toString())
    }

    private fun confirmPaiement() {
        AlertDialog.Builder(this)
            .setTitle("Confirmer le paiement")
            .setMessage(
                String.format(Locale.FRANCE, "Montant : %.2f DT\n\n", frais) +
                        "Paiement simulé : aucun argent n'est prélevé pour le moment."
            )
            .setPositiveButton("Payer") { _, _ -> viewModel.payer() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun loadPhoto(uri: Uri, store: (ByteArray) -> Unit) {
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                ImageUtils.compressToJpeg(this@DossierConducteurActivity, uri)
            }
            if (bytes == null) {
                toast("Image illisible, choisissez-en une autre")
                return@launch
            }
            store(bytes)
            renderPhotos()
        }
    }

    private fun renderPhotos() {
        showPhoto(binding.ivCin, binding.btnPhotoCin, viewModel.cinPhoto, "Photo du CIN")
        showPhoto(binding.ivPermis, binding.btnPhotoPermis, viewModel.permisPhoto, "Photo du permis")
        showPhoto(binding.ivVehicule, binding.btnPhotoVehicule, viewModel.vehiculePhoto, "Photo du véhicule")
        if (viewModel.cinPhoto != null || viewModel.permisPhoto != null || viewModel.vehiculePhoto != null) {
            binding.tvPhotoErreur.visibility = View.GONE
        }
    }

    private fun showPhoto(
        image: ImageView,
        button: MaterialButton,
        bytes: ByteArray?,
        label: String
    ) {
        if (bytes == null) {
            image.setImageDrawable(null)
            button.text = label
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
            button.text = "✓ $label ajoutée"
        }
    }

    private fun clearErrors() {
        binding.etCin.error = null
        binding.etPermis.error = null
        binding.etMarque.error = null
        binding.etModel.error = null
        binding.etMatricule.error = null
        binding.etPlaces.error = null
        binding.tvPhotoErreur.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.btnEnvoyer.isEnabled = !loading
        binding.btnPayer.isEnabled = !loading
    }
}
