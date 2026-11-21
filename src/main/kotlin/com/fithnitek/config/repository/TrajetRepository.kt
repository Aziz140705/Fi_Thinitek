package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import com.fithnitek.models.TrajetDto
import com.fithnitek.models.TrajetRequest
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Locale

object TrajetRepository {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private const val SELECT_COLUMNS = """
        SELECT idTrajet, idConducteur, depart, destination, depart_lat, depart_lng, dest_lat, dest_lng,
               date_depart, prix, places_disponibles
        FROM `trajet`
    """

    private fun mapRow(rs: ResultSet): TrajetDto {
        val ts = rs.getTimestamp("date_depart")
        val dateStr = if (ts != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ts) else ""
        val timeStr = if (ts != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts) else ""

        val dLat = rs.getDouble("depart_lat").takeIf { !rs.wasNull() }
        val dLng = rs.getDouble("depart_lng").takeIf { !rs.wasNull() }
        val destL = rs.getDouble("dest_lat").takeIf { !rs.wasNull() }
        val destG = rs.getDouble("dest_lng").takeIf { !rs.wasNull() }

        return TrajetDto(
            idTrajet = rs.getInt("idTrajet"),
            idConducteur = rs.getInt("idConducteur"),
            depart = rs.getString("depart"),
            destination = rs.getString("destination"),
            departLat = dLat,
            departLng = dLng,
            destLat = destL,
            destLng = destG,
            dateDepart = dateStr,
            heureDepart = timeStr,
            prix = rs.getDouble("prix"),
            placesDisponibles = rs.getInt("places_disponibles")
        )
    }

    fun create(idConducteur: Int, req: TrajetRequest): TrajetDto? {
        val sql = """
            INSERT INTO `trajet`
            (idConducteur, depart, destination, depart_lat, depart_lng, dest_lat, dest_lng, date_depart, prix, places_total, places_disponibles, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'OUVERT')
        """.trimIndent()

        val fullDateStr = "${req.dateDepart.trim()} ${req.heureDepart.trim()}"
        val timestamp = try {
            val parsed = dateTimeFormat.parse(fullDateStr)
            Timestamp(parsed.time)
        } catch (e: Exception) {
            Timestamp(System.currentTimeMillis())
        }

        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { st ->
                st.setInt(1, idConducteur)
                st.setString(2, req.depart)
                st.setString(3, req.destination)

                if (req.departLat != null) st.setDouble(4, req.departLat) else st.setNull(4, Types.DOUBLE)
                if (req.departLng != null) st.setDouble(5, req.departLng) else st.setNull(5, Types.DOUBLE)
                if (req.destLat != null) st.setDouble(6, req.destLat) else st.setNull(6, Types.DOUBLE)
                if (req.destLng != null) st.setDouble(7, req.destLng) else st.setNull(7, Types.DOUBLE)

                st.setTimestamp(8, timestamp)
                st.setDouble(9, req.prix)
                st.setInt(10, req.placesDisponibles)
                st.setInt(11, req.placesDisponibles)

                st.executeUpdate()

                val newId = st.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1) else null
                } ?: return null

                TrajetDto(
                    idTrajet = newId,
                    idConducteur = idConducteur,
                    depart = req.depart,
                    destination = req.destination,
                    dateDepart = req.dateDepart,
                    heureDepart = req.heureDepart,
                    prix = req.prix,
                    placesDisponibles = req.placesDisponibles,
                    departLat = req.departLat,
                    departLng = req.departLng,
                    destLat = req.destLat,
                    destLng = req.destLng,
                    description = req.description
                )
            }
        }
    }

    // Mes trajets publiés (les publications annulées ne sont plus affichées)
    fun findByConducteur(idConducteur: Int): List<TrajetDto> {
        val sql = "$SELECT_COLUMNS WHERE idConducteur = ? AND (statut = 'OUVERT' OR statut IS NULL) ORDER BY date_depart DESC"

        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setInt(1, idConducteur)
                st.executeQuery().use { rs ->
                    val list = mutableListOf<TrajetDto>()
                    while (rs.next()) list.add(mapRow(rs))
                    list
                }
            }
        }
    }

    fun searchAll(): List<TrajetDto> {
        val sql = "$SELECT_COLUMNS WHERE (statut = 'OUVERT' OR statut IS NULL) AND places_disponibles > 0 ORDER BY date_depart ASC"
        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.executeQuery().use { rs ->
                    val list = mutableListOf<TrajetDto>()
                    while (rs.next()) list.add(mapRow(rs))
                    list
                }
            }
        }
    }

    // Trajets proposés au passager : ouverts, pas passés, avec des places, et pas les siens
    fun searchAll(idUser: Int): List<TrajetDto> {
        val sql = """
            $SELECT_COLUMNS
            WHERE (statut = 'OUVERT' OR statut IS NULL) AND places_disponibles > 0 AND date_depart > ? AND idConducteur <> ?
            ORDER BY date_depart ASC
        """.trimIndent()

        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                st.setInt(2, idUser)
                st.executeQuery().use { rs ->
                    val list = mutableListOf<TrajetDto>()
                    while (rs.next()) list.add(mapRow(rs))
                    list
                }
            }
        }
    }

    // Annuler la publication : le trajet disparaît et les demandes en cours sont annulées
    fun annuler(idTrajet: Int, idConducteur: Int): OpResult<Unit> {
        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val row = conn.prepareStatement(
                    "SELECT idConducteur, statut FROM `trajet` WHERE idTrajet = ? FOR UPDATE"
                ).use { st ->
                    st.setInt(1, idTrajet)
                    st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) to rs.getString(2) else null }
                }

                if (row == null) {
                    conn.rollback()
                    return OpResult.Fail(404, "Trajet introuvable")
                }
                if (row.first != idConducteur) {
                    conn.rollback()
                    return OpResult.Fail(403, "Ce trajet ne vous appartient pas")
                }
                if (row.second == "ANNULE") {
                    conn.rollback()
                    return OpResult.Fail(409, "Cette publication est déjà annulée")
                }

                conn.prepareStatement(
                    "UPDATE `reservation` SET statut = 'ANNULEE' WHERE idTrajet = ? AND statut IN ('EN_ATTENTE', 'ACCEPTEE')"
                ).use { st ->
                    st.setInt(1, idTrajet)
                    st.executeUpdate()
                }
                conn.prepareStatement("UPDATE `trajet` SET statut = 'ANNULE' WHERE idTrajet = ?").use { st ->
                    st.setInt(1, idTrajet)
                    st.executeUpdate()
                }

                conn.commit()
                return OpResult.Ok(Unit)
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }
}
