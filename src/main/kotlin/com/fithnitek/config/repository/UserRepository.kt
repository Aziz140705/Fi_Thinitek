
package com.fithnitek.repository

import com.fithnitek.config.DatabaseFactory
import org.mindrot.jbcrypt.BCrypt
import java.sql.Statement
import java.sql.SQLIntegrityConstraintViolationException

object UserRepository {

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
            null // Email déjà utilisé ou autre contrainte SQL
        }
    }
}
