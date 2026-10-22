import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "br.com.sobraai.designsystem"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    iosArm64()
    iosSimulatorArm64()
    sourceSets.commonMain.dependencies {
        implementation(project(":shared"))
        implementation(compose.components.resources)
        implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
        implementation("org.jetbrains.compose.foundation:foundation:1.10.0")
        implementation("org.jetbrains.compose.ui:ui:1.10.0")
        implementation("org.jetbrains.compose.material3:material3:1.9.0")
        implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    }
}

compose.resources { packageOfResClass = "br.com.sobraai.designsystem.resources" }
