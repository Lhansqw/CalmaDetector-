import java.util.Properties

plugins {
    id("com.android.application") version "9.5.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
}

// Clave de Groq para el asistente, solo para pruebas: se lee de local.properties (no se sube a git)
// y queda dentro de la APK, así que esa APK no se comparte.
val groqApiKey: String = Properties().run {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    getProperty("GROQ_API_KEY").orEmpty()
}

android {
    namespace = "com.example.calma"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.calma"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // La misma APK corre en teléfono y reloj a propósito: el teléfono usa Material 3
        // y el reloj Wear Material 3. Play Store no acepta esta APK para Wear; para
        // publicar habría que separar la app en módulos :phone y :wear.
        disable += setOf("InvalidWearFeatureAttribute", "WearMaterialTheme")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.1")
    implementation("androidx.core:core-ktx:1.19.1")

    // Wear OS Compose Material 3 & Foundation
    implementation("androidx.wear.compose:compose-material3:1.7.2")
    implementation("androidx.wear.compose:compose-foundation:1.7.2")
    implementation("androidx.wear.compose:compose-ui-tooling:1.7.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
