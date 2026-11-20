package com.example.fithnitekmobile

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.example.fithnitekmobile.databinding.ActivityProfilBinding
import com.example.fithnitekmobile.models.ProfilDto
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.ProfilViewModel

class ProfilActivity : BaseActivity() {

    private lateinit var binding: ActivityProfilBinding
    private val viewModel: ProfilViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }

        binding.btnEnregistrer.setOnClickListener {
            binding.etNom.error = null
            binding.etPrenom.error = null
            binding.etTelephone.error = null

            viewModel.enregistrer(
                nom = binding.etNom.text.toString(),
                prenom = binding.etPrenom.text.toString(),
                telephone = binding.etTelephone.text.toString()
            )
        }

        observeViewModel()
        viewModel.load()
    }

    private fun observeViewModel() {
        viewModel.profil.observe(this) { state ->
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
                    FormField.NOM -> binding.etNom.error = error.message
                    FormField.PRENOM -> binding.etPrenom.error = error.message
                    FormField.TELEPHONE -> binding.etTelephone.error = error.message
                    else -> toast(error.message)
                }
            }
        }

        viewModel.saveResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    toast("Profil mis à jour avec succès !", long = true)
                    render(state.data)
                }
                is Resource.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
                null -> Unit
            }
        }
    }

    private fun render(p: ProfilDto) {
        val initialNom = p.nom.take(1).uppercase()
        val initialPrenom = p.prenom.take(1).uppercase()
        binding.tvInitiales.text = "$initialNom$initialPrenom"
        binding.tvNomComplet.text = "${p.prenom} ${p.nom}"
        binding.tvRole.text = "Rôle : ${p.role}"
        binding.etEmail.setText(p.email)

        if (binding.etNom.text.isNullOrEmpty()) binding.etNom.setText(p.nom)
        if (binding.etPrenom.text.isNullOrEmpty()) binding.etPrenom.setText(p.prenom)
        if (binding.etTelephone.text.isNullOrEmpty() && p.telephone != null) binding.etTelephone.setText(p.telephone)
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.btnEnregistrer.isEnabled = !loading
    }
}
