package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import com.fithnitek.models.ProfilDto
import org.mindrot.jbcrypt.BCrypt
import java.sql.Statement
import java.sql.SQLIntegrityConstraintViolationException

object UserRepository {

    // US01 : Inscription
    fun register(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        telephone: String?
    ): Int? {

        val sql = """
            INSERT INTO `user`
            (nom, prenom, email, password_hash, telephone, role)
            VALUES (?, ?, ?, ?, ?, 'PASSAGER')
        """.trimIndent()

        val hash = BCrypt.hashpw(password, BCrypt.gensalt(12))

        return try {
            DatabaseFactory.getConnection().use { connection ->
                connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
                ).use { statement ->

                    statement.setString(1, nom)
                    statement.setString(2, prenom)
                    statement.setString(3, email)
                    statement.setString(4, hash)
                    statement.setString(5, telephone)

                    statement.executeUpdate()

                    statement.generatedKeys.use { keys ->
                        if (keys.next()) keys.getInt(1) else null
                    }
                }
            }
        } catch (e: SQLIntegrityConstraintViolationException) {
            null
        }
    }

    // US02 : Connexion
    fun login(email: String, password: String): Pair<Int, String>? {

        val sql = """
            SELECT idUser, password_hash, role
            FROM `user`
            WHERE email = ?
        """.trimIndent()

        return DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->

                statement.setString(1, email.trim().lowercase())

                statement.executeQuery().use { result ->
                    if (!result.next()) return null

                    val hash = result.getString("password_hash")
                        ?: return null

                    val isMatch = try {
                        BCrypt.checkpw(password, hash)
                    } catch (e: Exception) {
                        false
                    }

                    if (!isMatch) return null

                    val idUser = result.getInt("idUser")
                    val role = result.getString("role")

                    Pair(idUser, role)
                }
            }
        }
    }

    // Sprint 2 : lire le rôle actuel (source de vérité = base de données)
    fun getRole(idUser: Int): String? {
        return DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                "SELECT role FROM `user` WHERE idUser = ?"
            ).use { statement ->
                statement.setInt(1, idUser)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("role") else null
                }
            }
        }
    }

    // Sprint 2 : changer le rôle (ex. PASSAGER -> CONDUCTEUR)
    fun updateRole(idUser: Int, role: String): Boolean {
        return DatabaseFactory.getConnection().use { connection ->
            connection.prepareStatement(
                "UPDATE `user` SET role = ? WHERE idUser = ?"
            ).use { statement ->
                statement.setString(1, role)
                statement.setInt(2, idUser)
                statement.executeUpdate() > 0
            }
        }
    }

    fun findProfil(idUser: Int): ProfilDto? {
        val sql = "SELECT idUser, nom, prenom, email, telephone, role FROM `user` WHERE idUser = ?"
        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setInt(1, idUser)
                st.executeQuery().use { rs ->
                    if (rs.next()) {
                        ProfilDto(
                            idUser = rs.getInt("idUser"),
                            nom = rs.getString("nom"),
                            prenom = rs.getString("prenom"),
                            email = rs.getString("email"),
                            telephone = rs.getString("telephone"),
                            role = rs.getString("role")
                        )
                    } else null
                }
            }
        }
    }

    fun updateProfil(idUser: Int, nom: String, prenom: String, telephone: String?): Boolean {
        val sql = "UPDATE `user` SET nom = ?, prenom = ?, telephone = ? WHERE idUser = ?"
        return DatabaseFactory.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setString(1, nom)
                st.setString(2, prenom)
                st.setString(3, telephone)
                st.setInt(4, idUser)
                st.executeUpdate() > 0
            }
        }
    }
}
