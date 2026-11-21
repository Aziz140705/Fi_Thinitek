package com.fithnitek.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.DriverManager

object DatabaseFactory {

    // Paramètres MySQL : variables d'environnement, avec les valeurs XAMPP/WAMP par défaut.
    private fun env(name: String, default: String) =
        System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

    private val host get() = env("DB_HOST", "localhost")
    private val port get() = env("DB_PORT", "3306")
    private val dbName get() = env("DB_NAME", "fi_thnitek")
    private val dbUser get() = env("DB_USER", "root")
    private val dbPassword get() = System.getenv("DB_PASSWORD") ?: ""

    // Pool de connexions MySQL
    private var dataSource: HikariDataSource? = null

    // Initialisation de la base de données
    fun init() {
        try {
            createDatabaseIfNotExists()

            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://$host:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                username = dbUser
                password = dbPassword
                driverClassName = "com.mysql.cj.jdbc.Driver"

                maximumPoolSize = 5
                minimumIdle = 2
                connectionTimeout = 5000
                validationTimeout = 3000
            }

            val pool = HikariDataSource(config)

            pool.connection.use { connection ->
                println("Connexion MySQL réussie !")
                println("Base : ${connection.catalog}")
                createTablesIfNotExist(connection)
            }
            dataSource = pool
        } catch (e: Exception) {
            println("Attention : Impossible de se connecter à la base MySQL ($host:$port). ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createDatabaseIfNotExists() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            DriverManager.getConnection(
                "jdbc:mysql://$host:$port/?useSSL=false&allowPublicKeyRetrieval=true",
                dbUser,
                dbPassword
            ).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(
                        "CREATE DATABASE IF NOT EXISTS `$dbName` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
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

                // Table trajet (provisoire : sera remplacée par `voyages`)
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

                // Table reservation (provisoire : sera remplacée par `reservations`)
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

                // Table conducteur : dossier du conducteur (même structure que ta base)
                // statut : PENDING = dossier vérifié, en attente de paiement ; VALIDATED = payé, conducteur actif
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `conducteur` (
                        `idConducteur` INT AUTO_INCREMENT PRIMARY KEY,
                        `idUser` INT NOT NULL,
                        `cin_numero` VARCHAR(20) NOT NULL,
                        `cin_photo` VARCHAR(255) NOT NULL,
                        `permis_numero` VARCHAR(50) NOT NULL,
                        `permis_photo` VARCHAR(255) NOT NULL,
                        `statut` ENUM('PENDING','VALIDATED','REJECTED') NOT NULL DEFAULT 'PENDING',
                        UNIQUE KEY `idUser` (`idUser`),
                        UNIQUE KEY `uq_conducteur_cin` (`cin_numero`),
                        FOREIGN KEY (`idUser`) REFERENCES `user` (`idUser`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """.trimIndent()
                )

                // Table vehicule : 1 conducteur = 1 seule voiture (règle RG01)
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `vehicule` (
                        `idVehicule` INT AUTO_INCREMENT PRIMARY KEY,
                        `idConducteur` INT NOT NULL,
                        `matricule` VARCHAR(20) NOT NULL,
                        `model` VARCHAR(50) NOT NULL,
                        `marque` VARCHAR(50) NOT NULL,
                        `couleur` VARCHAR(20) DEFAULT NULL,
                        `nbplaces` INT NOT NULL,
                        `nb_sieges_enfant` INT NOT NULL DEFAULT 0,
                        `vehicule_photo` VARCHAR(255) DEFAULT NULL,
                        UNIQUE KEY `idConducteur` (`idConducteur`),
                        UNIQUE KEY `matricule` (`matricule`),
                        FOREIGN KEY (`idConducteur`) REFERENCES `conducteur` (`idConducteur`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                    """.trimIndent()
                )

                // Table paiement : historique de tous les paiements (nouveau)
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `paiement` (
                        `idPaiement` INT AUTO_INCREMENT PRIMARY KEY,
                        `idUser` INT NOT NULL,
                        `objet` VARCHAR(30) NOT NULL DEFAULT 'ACTIVATION_CONDUCTEUR',
                        `montant` DECIMAL(8,2) NOT NULL,
                        `methode` VARCHAR(20) NOT NULL DEFAULT 'SIMULE',
                        `reference` VARCHAR(60) NOT NULL,
                        `statut` ENUM('PAID','FAILED') NOT NULL DEFAULT 'PAID',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY `uq_paiement_reference` (`reference`),
                        INDEX `idx_paiement_user` (`idUser`),
                        FOREIGN KEY (`idUser`) REFERENCES `user` (`idUser`) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent()
                )
            }
            ensureTrajetStatut(connection)
            println("Tables 'user', 'trajet', 'reservation', 'conducteur', 'vehicule' et 'paiement' prêtes.")
        } catch (e: Exception) {
            println("Avertissement création tables: ${e.message}")
        }
    }
    // Ajoute la colonne `statut` aux trajets (OUVERT ou ANNULE) si elle n'existe pas encore
    private fun ensureTrajetStatut(connection: Connection) {
        try {
            connection.createStatement().use { st ->
                st.executeUpdate("ALTER TABLE `trajet` ADD COLUMN `statut` VARCHAR(20) NOT NULL DEFAULT 'OUVERT'")
            }
            println("Colonne trajet.statut ajoutée.")
        } catch (e: Exception) {
            // La colonne existe déjà : rien à faire
        }
    }
    // Fournir une connexion aux repositories
    fun getConnection(): Connection {
        val ds = dataSource ?: throw IllegalStateException("La base de données n'est pas connectée. Assurez-vous que MySQL est démarré ($host:$port).")
        return ds.connection
    }

    // Fermer le pool de connexions
    fun close() {
        dataSource?.close()
        dataSource = null
    }
}