package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import com.fithnitek.models.ReservationDto
import com.fithnitek.models.ReservationRequest
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ReservationRepository {

    // Le passager ne peut plus annuler sa réservation à moins de 24 h du départ
    private const val DELAI_ANNULATION_HEURES = 24L

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    private const val SELECT_BASE = """
        SELECT r.idReservation, r.idTrajet, r.idPassager, r.nb_places, r.statut, r.prix_total,
               t.depart, t.destination, t.date_depart,
               CONCAT(u.prenom, ' ', u.nom) AS passager_nom
        FROM `reservation` r
        JOIN `trajet` t ON t.idTrajet = r.idTrajet
        JOIN `user` u ON u.idUser = r.idPassager
    """

    private fun mapRow(rs: ResultSet): ReservationDto {
        val date = rs.getTimestamp("date_depart")?.toLocalDateTime()
        return ReservationDto(
            idReservation = rs.getInt("idReservation"),
            idTrajet = rs.getInt("idTrajet"),
            idPassager = rs.getInt("idPassager"),
            placesReservees = rs.getInt("nb_places"),
            statut = rs.getString("statut"),
            depart = rs.getString("depart"),
            destination = rs.getString("destination"),
            dateDepart = date?.format(dateFmt) ?: "",
            heureDepart = date?.format(timeFmt) ?: "",
            prixTotal = rs.getDouble("prix_total"),
            passagerNom = rs.getString("passager_nom")
        )
    }

    private fun findById(conn: Connection, id: Int): ReservationDto? =
        conn.prepareStatement("$SELECT_BASE WHERE r.idReservation = ?").use { st ->
            st.setInt(1, id)
            st.executeQuery().use { rs -> if (rs.next()) mapRow(rs) else null }
        }

    private fun list(sql: String, id: Int): List<ReservationDto> =
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setInt(1, id)
                st.executeQuery().use { rs ->
                    val out = mutableListOf<ReservationDto>()
                    while (rs.next()) out.add(mapRow(rs))
                    out
                }
            }
        }

    // Mes réservations (passager)
    fun findMesReservations(idPassager: Int): List<ReservationDto> =
        list("$SELECT_BASE WHERE r.idPassager = ? ORDER BY r.created_at DESC", idPassager)

    // Demandes reçues sur mes trajets (conducteur) : les demandes en attente d'abord
    fun findReservationsRecues(idConducteur: Int): List<ReservationDto> =
        list(
            "$SELECT_BASE WHERE t.idConducteur = ? ORDER BY FIELD(r.statut, 'EN_ATTENTE') DESC, r.created_at DESC",
            idConducteur
        )

    private class TrajetVerrou(
        val idConducteur: Int,
        val prix: Double,
        val placesDispo: Int,
        val date: LocalDateTime,
        val statut: String
    )

    // ---------- Réserver : les places sont retenues tout de suite ----------
    fun creer(idPassager: Int, req: ReservationRequest): OpResult<ReservationDto> {
        if (req.placesReservees < 1 || req.placesReservees > 8) {
            return OpResult.Fail(400, "Nombre de places invalide")
        }

        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val trajet = conn.prepareStatement(
                    "SELECT idConducteur, prix, places_disponibles, date_depart, statut FROM `trajet` WHERE idTrajet = ? FOR UPDATE"
                ).use { st ->
                    st.setInt(1, req.idTrajet)
                    st.executeQuery().use { rs ->
                        if (rs.next()) TrajetVerrou(
                            idConducteur = rs.getInt("idConducteur"),
                            prix = rs.getDouble("prix"),
                            placesDispo = rs.getInt("places_disponibles"),
                            date = rs.getTimestamp("date_depart").toLocalDateTime(),
                            statut = rs.getString("statut")
                        ) else null
                    }
                }

                if (trajet == null) {
                    conn.rollback()
                    return OpResult.Fail(404, "Trajet introuvable")
                }
                if (trajet.statut != "OUVERT") {
                    conn.rollback()
                    return OpResult.Fail(409, "Cette publication a été annulée")
                }
                if (trajet.idConducteur == idPassager) {
                    conn.rollback()
                    return OpResult.Fail(403, "Vous ne pouvez pas réserver votre propre trajet")
                }
                if (trajet.date.isBefore(LocalDateTime.now())) {
                    conn.rollback()
                    return OpResult.Fail(409, "Ce trajet est déjà passé")
                }
                if (trajet.placesDispo <= 0) {
                    conn.rollback()
                    return OpResult.Fail(409, "Trajet complet")
                }
                if (trajet.placesDispo < req.placesReservees) {
                    conn.rollback()
                    return OpResult.Fail(409, "Seulement ${trajet.placesDispo} place(s) disponible(s)")
                }

                val doublons = conn.prepareStatement(
                    "SELECT COUNT(*) FROM `reservation` WHERE idTrajet = ? AND idPassager = ? AND statut IN ('EN_ATTENTE', 'ACCEPTEE')"
                ).use { st ->
                    st.setInt(1, req.idTrajet)
                    st.setInt(2, idPassager)
                    st.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
                }
                if (doublons > 0) {
                    conn.rollback()
                    return OpResult.Fail(409, "Vous avez déjà une réservation active sur ce trajet")
                }

                val prixTotal = trajet.prix * req.placesReservees

                val newId = conn.prepareStatement(
                    "INSERT INTO `reservation` (idTrajet, idPassager, nb_places, statut, prix_total) VALUES (?, ?, ?, 'EN_ATTENTE', ?)",
                    Statement.RETURN_GENERATED_KEYS
                ).use { st ->
                    st.setInt(1, req.idTrajet)
                    st.setInt(2, idPassager)
                    st.setInt(3, req.placesReservees)
                    st.setDouble(4, prixTotal)
                    st.executeUpdate()
                    st.generatedKeys.use { k -> if (k.next()) k.getInt(1) else error("ID non généré") }
                }

                changePlaces(conn, req.idTrajet, -req.placesReservees)

                conn.commit()
                return OpResult.Ok(findById(conn, newId)!!)
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    // ---------- Changements d'état ----------
    private class Verrouillee(
        val idReservation: Int,
        val idTrajet: Int,
        val idPassager: Int,
        val nbPlaces: Int,
        val statut: String,
        val idConducteur: Int,
        val date: LocalDateTime
    )

    private fun setStatut(conn: Connection, id: Int, statut: String) {
        conn.prepareStatement("UPDATE `reservation` SET statut = ? WHERE idReservation = ?").use { st ->
            st.setString(1, statut)
            st.setInt(2, id)
            st.executeUpdate()
        }
    }

    // delta négatif = retenir des places, positif = en rendre
    private fun changePlaces(conn: Connection, idTrajet: Int, delta: Int) {
        conn.prepareStatement(
            "UPDATE `trajet` SET places_disponibles = LEAST(places_total, places_disponibles + ?) WHERE idTrajet = ?"
        ).use { st ->
            st.setInt(1, delta)
            st.setInt(2, idTrajet)
            st.executeUpdate()
        }
    }

    // Verrouille toujours le trajet d'abord, puis la réservation (même ordre partout)
    private fun transition(
        idReservation: Int,
        block: (Connection, Verrouillee) -> OpResult.Fail?
    ): OpResult<ReservationDto> {
        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val idTrajet = conn.prepareStatement(
                    "SELECT idTrajet FROM `reservation` WHERE idReservation = ?"
                ).use { st ->
                    st.setInt(1, idReservation)
                    st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else null }
                }
                if (idTrajet == null) {
                    conn.rollback()
                    return OpResult.Fail(404, "Réservation introuvable")
                }

                val trajet = conn.prepareStatement(
                    "SELECT idConducteur, date_depart FROM `trajet` WHERE idTrajet = ? FOR UPDATE"
                ).use { st ->
                    st.setInt(1, idTrajet)
                    st.executeQuery().use { rs ->
                        if (rs.next()) rs.getInt(1) to rs.getTimestamp(2).toLocalDateTime() else null
                    }
                }
                if (trajet == null) {
                    conn.rollback()
                    return OpResult.Fail(404, "Trajet introuvable")
                }

                val locked = conn.prepareStatement(
                    "SELECT idPassager, nb_places, statut FROM `reservation` WHERE idReservation = ? FOR UPDATE"
                ).use { st ->
                    st.setInt(1, idReservation)
                    st.executeQuery().use { rs ->
                        if (rs.next()) Verrouillee(
                            idReservation = idReservation,
                            idTrajet = idTrajet,
                            idPassager = rs.getInt("idPassager"),
                            nbPlaces = rs.getInt("nb_places"),
                            statut = rs.getString("statut"),
                            idConducteur = trajet.first,
                            date = trajet.second
                        ) else null
                    }
                }
                if (locked == null) {
                    conn.rollback()
                    return OpResult.Fail(404, "Réservation introuvable")
                }

                val fail = block(conn, locked)
                if (fail != null) {
                    conn.rollback()
                    return fail
                }

                conn.commit()
                return OpResult.Ok(findById(conn, idReservation)!!)
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    // Le conducteur accepte une demande en attente
    fun accepter(idReservation: Int, idConducteur: Int): OpResult<ReservationDto> =
        transition(idReservation) { conn, r ->
            if (r.idConducteur != idConducteur)
                return@transition OpResult.Fail(403, "Cette demande ne concerne pas l'un de vos trajets")
            if (r.statut != "EN_ATTENTE")
                return@transition OpResult.Fail(409, "Cette demande n'est plus en attente")
            if (r.date.isBefore(LocalDateTime.now()))
                return@transition OpResult.Fail(409, "Ce trajet est déjà passé")
            setStatut(conn, r.idReservation, "ACCEPTEE")
            null
        }

    // Le conducteur refuse une demande en attente : les places sont rendues
    fun refuser(idReservation: Int, idConducteur: Int): OpResult<ReservationDto> =
        transition(idReservation) { conn, r ->
            if (r.idConducteur != idConducteur)
                return@transition OpResult.Fail(403, "Cette demande ne concerne pas l'un de vos trajets")
            if (r.statut != "EN_ATTENTE")
                return@transition OpResult.Fail(409, "Cette demande n'est plus en attente")
            setStatut(conn, r.idReservation, "REFUSEE")
            changePlaces(conn, r.idTrajet, r.nbPlaces)
            null
        }

    // Le passager annule sa réservation, sauf à moins de 24 h du départ : les places sont rendues
    fun annuler(idReservation: Int, idPassager: Int): OpResult<ReservationDto> =
        transition(idReservation) { conn, r ->
            if (r.idPassager != idPassager)
                return@transition OpResult.Fail(403, "Cette réservation ne vous appartient pas")
            if (r.statut != "EN_ATTENTE" && r.statut != "ACCEPTEE")
                return@transition OpResult.Fail(409, "Cette réservation ne peut plus être annulée")
            if (r.date.isBefore(LocalDateTime.now().plusHours(DELAI_ANNULATION_HEURES)))
                return@transition OpResult.Fail(409, "Annulation impossible : le départ a lieu dans moins de 24 h")
            setStatut(conn, r.idReservation, "ANNULEE")
            changePlaces(conn, r.idTrajet, r.nbPlaces)
            null
        }
}