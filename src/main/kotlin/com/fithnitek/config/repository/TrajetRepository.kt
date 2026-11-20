package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import com.fithnitek.models.TrajetDto
import com.fithnitek.models.TrajetRequest
import java.sql.Statement
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object TrajetRepository {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun create(idConducteur: Int, req: TrajetRequest): TrajetDto? {
        val sql = """
            INSERT INTO `trajet`
            (idConducteur, depart, destination, depart_lat, depart_lng, dest_lat, dest_lng, date_depart, prix, places_total, places_disponibles)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                st.setObject(4, req.departLat)
                st.setObject(5, req.departLng)
                st.setObject(6, req.destLat)
                st.setObject(7, req.destLng)
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

    fun findByConducteur(idConducteur: Int): List<TrajetDto> {
        val sql = """
            SELECT idTrajet, idConducteur, depart, destination, depart_lat, depart_lng, dest_lat, dest_lng, date_depart, prix, places_disponibles
            FROM `trajet`
            WHERE idConducteur = ?
            ORDER BY date_depart DESC
        """.trimIndent()

        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setInt(1, idConducteur)
                st.executeQuery().use { rs ->
                    val list = mutableListOf<TrajetDto>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("date_depart")
                        val dateStr = if (ts != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ts) else ""
                        val timeStr = if (ts != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts) else ""

                        list.add(
                            TrajetDto(
                                idTrajet = rs.getInt("idTrajet"),
                                idConducteur = rs.getInt("idConducteur"),
                                depart = rs.getString("depart"),
                                destination = rs.getString("destination"),
                                departLat = rs.getObject("depart_lat") as? Double,
                                departLng = rs.getObject("depart_lng") as? Double,
                                destLat = rs.getObject("dest_lat") as? Double,
                                destLng = rs.getObject("dest_lng") as? Double,
                                dateDepart = dateStr,
                                heureDepart = timeStr,
                                prix = rs.getDouble("prix"),
                                placesDisponibles = rs.getInt("places_disponibles")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    fun searchAll(): List<TrajetDto> {
        val sql = """
            SELECT idTrajet, idConducteur, depart, destination, depart_lat, depart_lng, dest_lat, dest_lng, date_depart, prix, places_disponibles
            FROM `trajet`
            WHERE places_disponibles > 0
            ORDER BY date_depart ASC
        """.trimIndent()

        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.executeQuery().use { rs ->
                    val list = mutableListOf<TrajetDto>()
                    while (rs.next()) {
                        val ts = rs.getTimestamp("date_depart")
                        val dateStr = if (ts != null) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ts) else ""
                        val timeStr = if (ts != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts) else ""

                        list.add(
                            TrajetDto(
                                idTrajet = rs.getInt("idTrajet"),
                                idConducteur = rs.getInt("idConducteur"),
                                depart = rs.getString("depart"),
                                destination = rs.getString("destination"),
                                departLat = rs.getObject("depart_lat") as? Double,
                                departLng = rs.getObject("depart_lng") as? Double,
                                destLat = rs.getObject("dest_lat") as? Double,
                                destLng = rs.getObject("dest_lng") as? Double,
                                dateDepart = dateStr,
                                heureDepart = timeStr,
                                prix = rs.getDouble("prix"),
                                placesDisponibles = rs.getInt("places_disponibles")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    fun delete(idTrajet: Int, idConducteur: Int): Boolean {
        val sql = "DELETE FROM `trajet` WHERE idTrajet = ? AND idConducteur = ?"
        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setInt(1, idTrajet)
                st.setInt(2, idConducteur)
                st.executeUpdate() > 0
            }
        }
    }
}
