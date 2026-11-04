
package com

import com.fithnitek.config.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args) // Démarrer Ktor
}

fun Application.module() {
    DatabaseFactory.init() // Connecter MySQL
    configureRouting()     // Activer les routes
}
