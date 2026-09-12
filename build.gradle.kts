// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("harnessUnit") {
    group = "verification"
    description = "Runs the fast local unit-test harness."
    dependsOn(":app:testDebugUnitTest")
}

tasks.register("harnessVerify") {
    group = "verification"
    description = "Runs unit tests and Android lint without requiring a device."
    dependsOn(":app:testDebugUnitTest", ":app:lintDebug")
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
