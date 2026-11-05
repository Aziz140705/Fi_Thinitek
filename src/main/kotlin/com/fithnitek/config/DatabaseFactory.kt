
package com.fithnitek.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object DatabaseFactory {

    // Pool de connexions MySQL
    private lateinit var dataSource: HikariDataSource

    // Initialisation de la base de données
    fun init() {

        val config = HikariConfig().apply {

            // Configuration MySQL
            jdbcUrl = "jdbc:mysql://localhost:3306/fi_thnitek"
            username = "root"
            password = ""
            driverClassName = "com.mysql.cj.jdbc.Driver"

            // Configuration du pool HikariCP
            maximumPoolSize = 5
            minimumIdle = 2
            connectionTimeout = 30000
        }

        // Créer le pool de connexions
        dataSource = HikariDataSource(config)

        // Tester la connexion MySQL
        dataSource.connection.use { connection ->
            println("Connexion MySQL reussie !")
            println("Base : ${connection.catalog}")
        }
    }

    // Fournir une connexion aux repositories
    fun getConnection(): Connection {
        return dataSource.connection
    }

    // Fermer le pool de connexions
    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}
