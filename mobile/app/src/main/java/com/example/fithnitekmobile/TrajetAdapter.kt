package com.example.fithnitekmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fithnitekmobile.databinding.ItemTrajetBinding
import com.example.fithnitekmobile.models.TrajetDto

class TrajetAdapter(
    private val onDeleteClick: ((TrajetDto) -> Unit)? = null,
    private val onReserveClick: ((TrajetDto) -> Unit)? = null
) : RecyclerView.Adapter<TrajetAdapter.TrajetViewHolder>() {

    private val items = mutableListOf<TrajetDto>()

    fun submitList(newList: List<TrajetDto>?) {
        items.clear()
        if (newList != null) {
            items.addAll(newList)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrajetViewHolder {
        val binding = ItemTrajetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrajetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrajetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TrajetViewHolder(private val binding: ItemTrajetBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(t: TrajetDto) {
            binding.tvRoute.text = "${t.depart} → ${t.destination}"
            binding.tvStatut.text = if (t.placesDisponibles > 0) "DISPONIBLE" else "COMPLET"
            binding.tvDate.text = "${t.dateDepart} à ${t.heureDepart}"
            binding.tvConducteur.text = "Conduite"
            binding.tvInfos.text = "%.1f DT / place • %d place(s) libre(s)".format(t.prix, t.placesDisponibles)

            if (onReserveClick != null) {
                binding.btnPrimary.visibility = View.VISIBLE
                binding.btnPrimary.text = "Réserver"
                binding.btnPrimary.isEnabled = t.placesDisponibles > 0
                binding.btnPrimary.setOnClickListener { onReserveClick.invoke(t) }
            } else {
                binding.btnPrimary.visibility = View.GONE
            }

            if (onDeleteClick != null) {
                binding.btnSecondary.visibility = View.VISIBLE
                binding.btnSecondary.text = "Supprimer"
                binding.btnSecondary.setOnClickListener { onDeleteClick.invoke(t) }
            } else {
                binding.btnSecondary.visibility = View.GONE
            }
        }
    }
}
