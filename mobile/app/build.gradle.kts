import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    // Cherche local.properties dans le dossier ouvert (racine) ET dans mobile/ (mobile/ est prioritaire)
    listOf(rootProject.file("local.properties"), rootProject.file("mobile/local.properties"), file("../local.properties"))
        .distinct()
        .filter { it.exists() }
        .forEach { f -> f.inputStream().use { load(it) } }
}
val cartoApiKey = localProperties.getProperty("CARTO_API_KEY", "")

// Adresse du backend Ktor. Pour la changer SANS modifier le code Kotlin, ajoutez dans
// mobile/local.properties :   API_BASE_URL=http://192.168.1.16:8080/
val apiBaseUrl = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")

android {
    namespace = "com.example.fithnitekmobile"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.fithnitekmobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "CARTO_API_KEY", "\"$cartoApiKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    // Map 3D 100% free - MapLibre
    implementation("org.maplibre.gl:android-sdk:11.0.0")

    // Retrofit : communication avec Ktor
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // MVVM : ViewModel, LiveData, coroutines (viewModelScope)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Listes (trajets, réservations)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}