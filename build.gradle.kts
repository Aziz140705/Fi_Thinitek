
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
}

group = "com"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Ktor - Backend
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)

    // Logs
    implementation(libs.logback.classic)

    // Connexion MySQL
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")

    // Tests
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)


// Recevoir et envoyer des données JSON
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.6.0")

// Convertir JSON en objets Kotlin
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.6.0")

// Hacher les mots de passe
    implementation("org.mindrot:jbcrypt:0.4")

    implementation("io.ktor:ktor-server-auth-jvm:3.6.0")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.6.0")


}
