package com.example.fithnitekmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fithnitekmobile.databinding.ItemReservationBinding
import com.example.fithnitekmobile.models.ReservationDto
import com.example.fithnitekmobile.models.ReservationMode

// Conducteur : boutons Accepter / Refuser sur les demandes en attente.
// Passager : bouton "Annuler la réservation" tant que la réservation est active.
class ReservationAdapter(
    private val mode: ReservationMode,
    private val onAccepter: (ReservationDto) -> Unit,
    private val onRefuser: (ReservationDto) -> Unit,
    private val onAnnuler: (ReservationDto) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.VH>() {

    private val items = mutableListOf<ReservationDto>()

    fun submitList(list: List<ReservationDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemReservationBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(r: ReservationDto) {
            b.tvRoute.text = "${r.depart} → ${r.destination}"
            b.tvStatut.text = when (r.statut) {
                "EN_ATTENTE" -> "EN ATTENTE"
                "ACCEPTEE" -> "ACCEPTÉE"
                "REFUSEE" -> "REFUSÉE"
                "ANNULEE" -> "ANNULÉE"
                else -> r.statut
            }
            b.tvDate.text = "${r.dateDepart} à ${r.heureDepart}"
            b.tvPersonne.text =
                if (mode == ReservationMode.CONDUCTEUR) "Passager : ${r.passagerNom ?: "-"}"
                else "Ma réservation"
            b.tvInfos.text = "%d place(s) • Total %.1f DT".format(r.placesReservees, r.prixTotal ?: 0.0)

            val attente = r.statut == "EN_ATTENTE"
            val active = r.statut == "EN_ATTENTE" || r.statut == "ACCEPTEE"

            if (mode == ReservationMode.CONDUCTEUR) {
                b.layoutActions.visibility = if (attente) View.VISIBLE else View.GONE
                b.btnAccepter.visibility = View.VISIBLE
                b.btnRefuser.visibility = View.VISIBLE
                b.btnAnnuler.visibility = View.GONE
            } else {
                b.layoutActions.visibility = if (active) View.VISIBLE else View.GONE
                b.btnAccepter.visibility = View.GONE
                b.btnRefuser.visibility = View.GONE
                b.btnAnnuler.visibility = View.VISIBLE
            }

            b.btnAccepter.setOnClickListener { onAccepter(r) }
            b.btnRefuser.setOnClickListener { onRefuser(r) }
            b.btnAnnuler.setOnClickListener { onAnnuler(r) }
        }
    }
}