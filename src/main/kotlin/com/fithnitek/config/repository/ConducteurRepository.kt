package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import com.fithnitek.config.PaiementService
import com.fithnitek.config.PhotoStorage
import com.fithnitek.models.DossierDto
import java.math.BigDecimal
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.Statement

sealed class OpResult<out T> {
    data class Ok<out T>(val value: T) : OpResult<T>()
    data class Fail(val httpStatus: Int, val message: String) : OpResult<Nothing>()
}

// Données déjà contrôlées (format) par la route. Le repository contrôle l'état en base.
class DossierInput(
    val cin: String,
    val permis: String,
    val marque: String,
    val model: String,
    val matricule: String,
    val couleur: String?,
    val nbPlaces: Int,
    val nbSiegesEnfant: Int,
    val cinPhoto: ByteArray,
    val permisPhoto: ByteArray,
    val vehiculePhoto: ByteArray
)

object ConducteurRepository {

    // ---------- Lire le dossier de l'utilisateur ----------
    fun find(idUser: Int, frais: BigDecimal): DossierDto =
        DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(
                """
                SELECT c.statut, v.marque, v.model, v.matricule, v.couleur, v.nbplaces, v.nb_sieges_enfant
                FROM conducteur c
                JOIN vehicule v ON v.idConducteur = c.idConducteur
                WHERE c.idUser = ?
                """.trimIndent()
            ).use { st ->
                st.setInt(1, idUser)
                st.executeQuery().use { rs ->
                    if (rs.next()) DossierDto(
                        existe = true,
                        statut = rs.getString("statut"),
                        marque = rs.getString("marque"),
                        model = rs.getString("model"),
                        matricule = rs.getString("matricule"),
                        couleur = rs.getString("couleur"),
                        nbPlaces = rs.getInt("nbplaces"),
                        nbSiegesEnfant = rs.getInt("nb_sieges_enfant"),
                        frais = frais.toDouble()
                    ) else DossierDto(existe = false, frais = frais.toDouble())
                }
            }
        }

    // Verrouille la ligne de l'utilisateur et vérifie qu'il est bien PASSAGER
    private fun checkPassager(conn: Connection, idUser: Int): OpResult.Fail? {
        val role = conn.prepareStatement("SELECT role FROM `user` WHERE idUser = ? FOR UPDATE").use { st ->
            st.setInt(1, idUser)
            st.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }
        return when (role) {
            null -> OpResult.Fail(404, "Utilisateur introuvable")
            "ADMIN" -> OpResult.Fail(403, "Un administrateur ne peut pas devenir conducteur")
            "CONDUCTEUR" -> OpResult.Fail(409, "Vous êtes déjà conducteur")
            else -> null
        }
    }

    private fun count(conn: Connection, sql: String, text: String, id: Int): Int =
        conn.prepareStatement(sql).use { st ->
            st.setString(1, text)
            st.setInt(2, id)
            st.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
        }

    // ---------- Déposer (ou remplacer) le dossier : statut PENDING ----------
    fun save(idUser: Int, d: DossierInput): OpResult<Unit> {
        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                checkPassager(conn, idUser)?.let { conn.rollback(); return it }

                val existing: Pair<Int, String>? = conn.prepareStatement(
                    "SELECT idConducteur, statut FROM conducteur WHERE idUser = ? FOR UPDATE"
                ).use { st ->
                    st.setInt(1, idUser)
                    st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) to rs.getString(2) else null }
                }
                if (existing?.second == "VALIDATED") {
                    conn.rollback()
                    return OpResult.Fail(409, "Votre dossier est déjà validé")
                }

                // Doublons : un CIN et une immatriculation ne peuvent appartenir qu'à un seul compte
                val myId = existing?.first ?: -1
                if (count(conn, "SELECT COUNT(*) FROM conducteur WHERE cin_numero = ? AND idConducteur <> ?", d.cin, myId) > 0) {
                    conn.rollback()
                    return OpResult.Fail(409, "Ce numéro de CIN est déjà utilisé par un autre compte")
                }
                if (count(conn, "SELECT COUNT(*) FROM vehicule WHERE matricule = ? AND idConducteur <> ?", d.matricule, myId) > 0) {
                    conn.rollback()
                    return OpResult.Fail(409, "Cette immatriculation est déjà enregistrée")
                }

                // Photos (elles remplacent les anciennes)
                val cinPath = PhotoStorage.save(idUser, "cin", d.cinPhoto)
                val permisPath = PhotoStorage.save(idUser, "permis", d.permisPhoto)
                val vehiculePath = PhotoStorage.save(idUser, "vehicule", d.vehiculePhoto)

                val idConducteur: Int = if (existing == null) {
                    conn.prepareStatement(
                        """
                        INSERT INTO conducteur (idUser, cin_numero, cin_photo, permis_numero, permis_photo, statut)
                        VALUES (?, ?, ?, ?, ?, 'PENDING')
                        """.trimIndent(),
                        Statement.RETURN_GENERATED_KEYS
                    ).use { st ->
                        st.setInt(1, idUser)
                        st.setString(2, d.cin)
                        st.setString(3, cinPath)
                        st.setString(4, d.permis)
                        st.setString(5, permisPath)
                        st.executeUpdate()
                        st.generatedKeys.use { k -> if (k.next()) k.getInt(1) else error("ID non généré") }
                    }
                } else {
                    conn.prepareStatement(
                        """
                        UPDATE conducteur
                        SET cin_numero = ?, cin_photo = ?, permis_numero = ?, permis_photo = ?, statut = 'PENDING'
                        WHERE idConducteur = ?
                        """.trimIndent()
                    ).use { st ->
                        st.setString(1, d.cin)
                        st.setString(2, cinPath)
                        st.setString(3, d.permis)
                        st.setString(4, permisPath)
                        st.setInt(5, existing.first)
                        st.executeUpdate()
                    }
                    existing.first
                }

                // Véhicule : on remplace l'ancien (un dossier PENDING n'a encore aucun voyage)
                conn.prepareStatement("DELETE FROM vehicule WHERE idConducteur = ?").use { st ->
                    st.setInt(1, idConducteur)
                    st.executeUpdate()
                }
                conn.prepareStatement(
                    """
                    INSERT INTO vehicule
                    (idConducteur, matricule, model, marque, couleur, nbplaces, nb_sieges_enfant, vehicule_photo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { st ->
                    st.setInt(1, idConducteur)
                    st.setString(2, d.matricule)
                    st.setString(3, d.model)
                    st.setString(4, d.marque)
                    st.setString(5, d.couleur)
                    st.setInt(6, d.nbPlaces)
                    st.setInt(7, d.nbSiegesEnfant)
                    st.setString(8, vehiculePath)
                    st.executeUpdate()
                }

                conn.commit()
                return OpResult.Ok(Unit)
            } catch (e: SQLIntegrityConstraintViolationException) {
                conn.rollback()
                return OpResult.Fail(409, "CIN ou immatriculation déjà utilisés par un autre compte")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    // ---------- Payer : dossier VALIDATED + rôle CONDUCTEUR, en UNE seule transaction ----------
    fun payer(idUser: Int, montant: BigDecimal, service: PaiementService): OpResult<Unit> {
        DatabaseFactory.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                checkPassager(conn, idUser)?.let { conn.rollback(); return it }

                val statut = conn.prepareStatement(
                    "SELECT statut FROM conducteur WHERE idUser = ? FOR UPDATE"
                ).use { st ->
                    st.setInt(1, idUser)
                    st.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
                }
                if (statut == null) {
                    conn.rollback()
                    return OpResult.Fail(409, "Déposez d'abord votre dossier conducteur")
                }
                if (statut != "PENDING") {
                    conn.rollback()
                    return OpResult.Fail(409, "Ce dossier ne peut pas être payé")
                }

                // Le paiement a lieu pendant que la ligne est verrouillée : impossible de payer deux fois
                val paiement = service.payer(idUser, montant)
                if (!paiement.success) {
                    conn.rollback()
                    return OpResult.Fail(402, paiement.message ?: "Paiement refusé")
                }

                conn.prepareStatement(
                    """
                    INSERT INTO paiement (idUser, objet, montant, methode, reference, statut)
                    VALUES (?, 'ACTIVATION_CONDUCTEUR', ?, ?, ?, 'PAID')
                    """.trimIndent()
                ).use { st ->
                    st.setInt(1, idUser)
                    st.setBigDecimal(2, montant)
                    st.setString(3, service.methode)
                    st.setString(4, paiement.reference)
                    st.executeUpdate()
                }
                conn.prepareStatement("UPDATE conducteur SET statut = 'VALIDATED' WHERE idUser = ?").use { st ->
                    st.setInt(1, idUser)
                    st.executeUpdate()
                }
                conn.prepareStatement("UPDATE `user` SET role = 'CONDUCTEUR' WHERE idUser = ?").use { st ->
                    st.setInt(1, idUser)
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
