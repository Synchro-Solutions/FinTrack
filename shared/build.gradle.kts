import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
}

// Groq API key: se lee de local.properties (gitignored, nunca se commitea) o de una
// variable de entorno GROQ_API_KEY (util para CI), nunca hardcodeada en el codigo fuente.
// A diferencia de webApp/EnvConfig esto NO es estricto: si falta, el build sigue igual
// (el chat IA y el plan de ahorro con IA simplemente no responden hasta que se agregue).
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

fun groqApiKeyValue(): String =
    System.getenv("GROQ_API_KEY")
        ?: localProperties.getProperty("groq.apiKey")
        ?: ""

val generatedGroqSecretsDir = layout.buildDirectory.dir("generated/groqSecrets/kotlin")

// groqApiKey se calcula ADENTRO del task (no arriba, a nivel de script) a proposito: la
// configuration cache no puede serializar una referencia a una propiedad top-level del
// script capturada desde doLast (Gradle lo reporta como "script object reference"). Como
// variable local del propio task, en cambio, se captura sin problema.
val generateGroqSecrets by tasks.registering {
    val groqApiKey = groqApiKeyValue()
    inputs.property("groqApiKey", groqApiKey)
    val outputDir = generatedGroqSecretsDir
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("fintrack/proyecto4/ai/GroqSecrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package fintrack.proyecto4.ai

            internal object GroqSecrets {
                const val API_KEY = "$groqApiKey"
            }

            """.trimIndent()
        )
    }
}

kotlin {
    /*listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }*/
    
    js {
        browser()
    }

    /*
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    */
    
    androidLibrary {
       namespace = "fintrack.proyecto4.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.androidx.activity.compose)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
            implementation(libs.mlkit.textRecognition)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain {
            kotlin.srcDir(generateGroqSecrets)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(compose.materialIconsExtended)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.haze)
            implementation(libs.haze.materials)
        }
        iosMain.dependencies {
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.auth)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.auth)
        }
    }
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
    androidRuntimeClasspath(libs.compose.uiTooling)
}