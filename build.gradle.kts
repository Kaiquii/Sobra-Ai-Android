// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.kmp.library) apply false
}

tasks.register("harnessUnit") {
    group = "verification"
    description = "Runs Android app and shared Kotlin unit tests on the JVM."
    dependsOn(":app:testDebugUnitTest", ":shared:testAndroidHostTest", ":sharedUi:testAndroidHostTest")
}

tasks.register("harnessVerify") {
    group = "verification"
    description = "Runs app/shared JVM tests and Android debug lint without a device."
    dependsOn("harnessUnit", ":app:lintDebug")
}

tasks.register("harnessDevice") {
    group = "verification"
    description = "Runs instrumented tests on a connected Android device or emulator."
    dependsOn(":app:connectedDebugAndroidTest")
}

tasks.register("harnessAll") {
    group = "verification"
    description = "Runs unit tests, lint, and instrumented Android tests."
    dependsOn("harnessVerify", "harnessDevice")
}
