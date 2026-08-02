import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Variables de Firebase/Google requeridas por main.kt. Se leen de webApp/.env
// (gitignored, ver webApp/.env.example) o de variables de entorno del sistema
// (útil para CI), nunca hardcodeadas en el código fuente.
val envProperties = Properties().apply {
    val envFile = file("$projectDir/.env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

fun envValue(key: String): String? =
    System.getenv(key)
        ?: envProperties.getProperty(key)?.takeIf { it.isNotBlank() }

fun requiredEnvValue(key: String) =
    providers.provider {
        envValue(key) ?: error(
            "Falta la variable de entorno '$key'. Copia webApp/.env.example a " +
                "webApp/.env y completa los valores (ver Firebase Console)."
        )
    }

val generatedEnvDir = layout.buildDirectory.dir("generated/env/kotlin")

// generateEnvConfig solo se crea/configura de verdad cuando algo depende de
// ella (ver dependsOn en compileKotlinJs, más abajo), así que "./gradlew lint"
// u otro comando que solo configure el proyecto sin compilar webApp no exige
// tener el .env. envValue() solo se evalúa en ese momento, no antes.
val generateEnvConfig by tasks.registering {
    val firebaseApiKey = requiredEnvValue("FIREBASE_API_KEY")
    val firebaseAppId = requiredEnvValue("FIREBASE_APP_ID")
    val firebaseProjectId = requiredEnvValue("FIREBASE_PROJECT_ID")
    val firebaseAuthDomain = requiredEnvValue("FIREBASE_AUTH_DOMAIN")
    val firebaseStorageBucket = requiredEnvValue("FIREBASE_STORAGE_BUCKET")
    val firebaseGcmSenderId = requiredEnvValue("FIREBASE_GCM_SENDER_ID")
    val googleWebClientId = requiredEnvValue("GOOGLE_WEB_CLIENT_ID")

    inputs.property("firebaseApiKey", firebaseApiKey)
    inputs.property("firebaseAppId", firebaseAppId)
    inputs.property("firebaseProjectId", firebaseProjectId)
    inputs.property("firebaseAuthDomain", firebaseAuthDomain)
    inputs.property("firebaseStorageBucket", firebaseStorageBucket)
    inputs.property("firebaseGcmSenderId", firebaseGcmSenderId)
    inputs.property("googleWebClientId", googleWebClientId)

    val outputDir = generatedEnvDir
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("fintrack/proyecto4/config/EnvConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package fintrack.proyecto4.config

            internal object EnvConfig {
                const val FIREBASE_API_KEY = "${firebaseApiKey.get()}"
                const val FIREBASE_APP_ID = "${firebaseAppId.get()}"
                const val FIREBASE_PROJECT_ID = "${firebaseProjectId.get()}"
                const val FIREBASE_AUTH_DOMAIN = "${firebaseAuthDomain.get()}"
                const val FIREBASE_STORAGE_BUCKET = "${firebaseStorageBucket.get()}"
                const val FIREBASE_GCM_SENDER_ID = "${firebaseGcmSenderId.get()}"
                const val GOOGLE_WEB_CLIENT_ID = "${googleWebClientId.get()}"
            }

            """.trimIndent()
        )
    }
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(libs.compose.ui)
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.app)
            implementation(libs.kotlinx.coroutines.core)
        }

        // srcDir aparte (sin pasar por el TaskProvider) para que resolver el
        // árbol de fuentes no fuerce la creación de generateEnvConfig.
        matching { it.name == "webMain" }.configureEach {
            kotlin.srcDir(generatedEnvDir)
        }
    }
}

// La dependencia real: solo al compilar webApp se ejecuta generateEnvConfig
// (y solo entonces se exige el .env).
tasks.matching { it.name == "compileKotlinJs" }.configureEach {
    dependsOn(generateEnvConfig)
}