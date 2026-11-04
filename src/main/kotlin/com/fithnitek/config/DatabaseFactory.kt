
package com.fithnitek.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object DatabaseFactory {

    private lateinit var dataSource: HikariDataSource

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://localhost:3306/fi_thnitek"
            username = "root"
            password = ""
            driverClassName = "com.mysql.cj.jdbc.Driver"

            maximumPoolSize = 5
            minimumIdle = 2
            connectionTimeout = 30000
        }

        dataSource = HikariDataSource(config)

        dataSource.connection.use { connection ->
            println("Connexion MySQL reussie !")
            println("Base : ${connection.catalog}")
        }
    }

    fun getConnection(): Connection = dataSource.connection

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}
