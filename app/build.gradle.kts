import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Configuración local NO versionada (local.properties):
//   ahgora.apiBaseUrl=https://taku-server.vercel.app/   (Servidor Ahgora; GET /api/alertas es público)
//   ahgora.demoMode=true                               (arranca con avisos de ejemplo rotulados)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String, default: String): String = (localProps.getProperty(key) ?: default).trim()
fun asBaseUrl(raw: String): String = if (raw.endsWith("/")) raw else "$raw/"

android {
    namespace = "bo.ahgora.comunidad"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "bo.ahgora.comunidad"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-hackathon"

        buildConfigField(
            "String", "API_BASE_URL",
            "\"${asBaseUrl(localProp("ahgora.apiBaseUrl", "https://taku-server.vercel.app/"))}\""
        )
        buildConfigField(
            "boolean", "DEMO_MODE_DEFAULT",
            localProp("ahgora.demoMode", "true").toBoolean().toString()
        )
    }

    buildTypes {
        release {
            // Para la hackathon el APK de release se firma con la clave de depuración y se instala directo.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.osmdroid.android)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
