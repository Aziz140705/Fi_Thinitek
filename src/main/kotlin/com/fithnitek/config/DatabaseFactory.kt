package com.fithnitek.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.DriverManager

object DatabaseFactory {

    // Pool de connexions MySQL
    private var dataSource: HikariDataSource? = null

    // Initialisation de la base de données
    fun init() {
        try {
            // 1. Création de la base si elle n'existe pas encore
            createDatabaseIfNotExists()

            val config = HikariConfig().apply {
                // Configuration MySQL
                jdbcUrl = "jdbc:mysql://localhost:3306/fi_thnitek?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
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

            // Tester la connexion MySQL et s'assurer que les tables existent
            pool.connection.use { connection ->
                println("Connexion MySQL réussie !")
                println("Base : ${connection.catalog}")
                createTablesIfNotExist(connection)
            }
            dataSource = pool
        } catch (e: Exception) {
            println("Attention : Impossible de se connecter à la base MySQL (localhost:3306). ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createDatabaseIfNotExists() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true",
                "root",
                ""
            ).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(
                        "CREATE DATABASE IF NOT EXISTS `fi_thnitek` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
                    )
                }
            }
        } catch (e: Exception) {
            println("Avertissement création BDD: ${e.message}")
        }
    }

    private fun createTablesIfNotExist(connection: Connection) {
        try {
            connection.createStatement().use { stmt ->
                // Table user
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `user` (
                        `idUser` INT AUTO_INCREMENT PRIMARY KEY,
                        `nom` VARCHAR(100) NOT NULL,
                        `prenom` VARCHAR(100) NOT NULL,
                        `email` VARCHAR(191) NOT NULL UNIQUE,
                        `password_hash` VARCHAR(255) NOT NULL,
                        `telephone` VARCHAR(20) DEFAULT NULL,
                        `role` VARCHAR(20) NOT NULL DEFAULT 'PASSAGER',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent()
                )

                // Table password_reset_tokens
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
                        `id` INT AUTO_INCREMENT PRIMARY KEY,
                        `idUser` INT NOT NULL,
                        `token_hash` VARCHAR(255) NOT NULL,
                        `expires_at` TIMESTAMP NOT NULL,
                        `used_at` TIMESTAMP NULL DEFAULT NULL,
                        `failed_attempts` INT DEFAULT 0,
                        FOREIGN KEY (`idUser`) REFERENCES `user` (`idUser`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent()
                )

                // Table trajet (Sprint 2) : trajets publiés par les conducteurs
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `trajet` (
                        `idTrajet` INT AUTO_INCREMENT PRIMARY KEY,
                        `idConducteur` INT NOT NULL,
                        `depart` VARCHAR(150) NOT NULL,
                        `destination` VARCHAR(150) NOT NULL,
                        `depart_lat` DOUBLE NULL,
                        `depart_lng` DOUBLE NULL,
                        `dest_lat` DOUBLE NULL,
                        `dest_lng` DOUBLE NULL,
                        `date_depart` DATETIME NOT NULL,
                        `prix` DECIMAL(8,2) NOT NULL,
                        `places_total` INT NOT NULL,
                        `places_disponibles` INT NOT NULL,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX `idx_trajet_date` (`date_depart`),
                        INDEX `idx_trajet_conducteur` (`idConducteur`),
                        FOREIGN KEY (`idConducteur`) REFERENCES `user` (`idUser`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent()
                )

                // Table reservation (Sprint 2) : demandes des passagers
                // statut : EN_ATTENTE, ACCEPTEE, REFUSEE, ANNULEE
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `reservation` (
                        `idReservation` INT AUTO_INCREMENT PRIMARY KEY,
                        `idTrajet` INT NOT NULL,
                        `idPassager` INT NOT NULL,
                        `nb_places` INT NOT NULL DEFAULT 1,
                        `statut` VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
                        `prix_total` DECIMAL(8,2) NOT NULL,
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX `idx_reservation_trajet` (`idTrajet`),
                        INDEX `idx_reservation_passager` (`idPassager`),
                        FOREIGN KEY (`idTrajet`) REFERENCES `trajet` (`idTrajet`) ON DELETE CASCADE,
                        FOREIGN KEY (`idPassager`) REFERENCES `user` (`idUser`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent()
                )
            }
            println("Tables 'user', 'password_reset_tokens', 'trajet' et 'reservation' prêtes et vérifiées.")
        } catch (e: Exception) {
            println("Avertissement création tables: ${e.message}")
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