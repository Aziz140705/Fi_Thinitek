package com.fithnitek.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object DatabaseFactory {

    // Pool de connexions MySQL
    private var dataSource: HikariDataSource? = null

    // Initialisation de la base de données
    fun init() {
        try {
            val config = HikariConfig().apply {
                // Configuration MySQL
                jdbcUrl = "jdbc:mysql://localhost:3306/fi_thnitek"
                username = "root"
                password = ""
                driverClassName = "com.mysql.cj.jdbc.Driver"

                // Configuration du pool HikariCP
                maximumPoolSize = 5
                minimumIdle = 2
                connectionTimeout = 5000
                validationTimeout = 3000
            }

            // Créer le pool de connexions
            val pool = HikariDataSource(config)

            // Tester la connexion MySQL
            pool.connection.use { connection ->
                println("Connexion MySQL réussie !")
                println("Base : ${connection.catalog}")
            }
            dataSource = pool
        } catch (e: Exception) {
            println("Attention : Impossible de se connecter à la base MySQL (localhost:3306). ${e.message}")
        }
    }

    // Fournir une connexion aux repositories
    fun getConnection(): Connection {
        val ds = dataSource ?: throw IllegalStateException("La base de données n'est pas connectée. Assurez-vous que MySQL est démarré sur localhost:3306.")
        return ds.connection
    }

    // Fermer le pool de connexions
    fun close() {
        dataSource?.close()
        dataSource = null
    }
}
