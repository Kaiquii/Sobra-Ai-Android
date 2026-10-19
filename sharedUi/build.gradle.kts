import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "br.com.sobraai.sharedui"
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        withHostTest {}
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "SobraAiUI"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.0")
            implementation("org.jetbrains.compose.ui:ui:1.10.0")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
