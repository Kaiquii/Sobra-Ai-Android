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
    description = "Runs app/shared/sharedUi JVM tests; this is a partial KMP check."
    dependsOn(":app:testDebugUnitTest", ":shared:testAndroidHostTest", ":sharedUi:testAndroidHostTest")
    doLast {
        logger.lifecycle("[harness] JVM concluida. Complete com harnessVerify e validacao Apple no Mac/iPhone.")
    }
}

tasks.register("harnessKmp") {
    group = "verification"
    description = "Compiles shared/sharedUi for Android and iOS, including both Apple test source sets."
    listOf(":shared", ":sharedUi").forEach { module ->
        dependsOn("$module:compileAndroidMain")
        listOf("IosArm64", "IosSimulatorArm64").forEach { target ->
            dependsOn("$module:compileKotlin$target", "$module:compileTestKotlin$target")
        }
    }
    doLast {
        logger.lifecycle("[harness] Fontes Android e KLIB iOS compilados. Isso nao executa testes Native, Swift, link Xcode ou UI.")
    }
}

tasks.register("harnessVerify") {
    group = "verification"
    description = "Verifies KMP sources, JVM tests, Android debug APK and lint without starting devices."
    dependsOn("harnessUnit", "harnessKmp", ":app:lintDebug", ":app:assembleDebug")
    doLast {
        logger.lifecycle("[harness] Verificacao local Android/KMP concluida. Build Swift/Xcode e execucao em iPhone exigem validacao Apple separada.")
    }
}

val harnessMacHost = providers.systemProperty("os.name").get().startsWith("Mac", ignoreCase = true)
val harnessArm64Host = providers.systemProperty("os.arch").get().lowercase() in setOf("aarch64", "arm64")

tasks.register("harnessIos") {
    group = "verification"
    description = "On macOS, verifies KMP and links debug Apple frameworks; does not run a simulator or build Swift."
    // Do not depend on disabled Native tasks on other hosts: the explicit Apple check must fail there.
    if (harnessMacHost) {
        dependsOn("harnessVerify")
        listOf(":shared", ":sharedUi").forEach { module ->
            dependsOn("$module:linkDebugFrameworkIosArm64", "$module:linkDebugFrameworkIosSimulatorArm64")
        }
    }
    doLast {
        check(harnessMacHost) { "harnessIos exige macOS/Xcode. Neste host use harnessVerify; validacao Apple permanece pendente." }
        logger.lifecycle("[harness] Frameworks Apple debug linkados. Compile tambem iosApp/SobraAi.xcodeproj para validar Swift e o host.")
    }
}

tasks.register("harnessIosTest") {
    group = "verification"
    description = "On macOS, runs shared/sharedUi Native tests on a prepared ARM64 simulator; invoke explicitly."
    if (harnessMacHost && harnessArm64Host) {
        dependsOn("harnessIos", ":shared:iosSimulatorArm64Test", ":sharedUi:iosSimulatorArm64Test")
    }
    doLast {
        check(harnessMacHost) { "harnessIosTest exige macOS/Xcode e simulador ARM64 preparado. Testes iOS nao foram executados." }
        check(harnessArm64Host) { "harnessIosTest exige Mac Apple Silicon com JDK ARM64; nao ha target de simulador Intel configurado." }
        logger.lifecycle("[harness] Testes Native concluidos; fluxos de UI e sensores ainda precisam de validacao em iPhone.")
    }
}

tasks.register("harnessDevice") {
    group = "verification"
    description = "Runs instrumented tests on a connected Android device or emulator."
    dependsOn(":app:connectedDebugAndroidTest")
}

tasks.register("harnessAll") {
    group = "verification"
    description = "Runs KMP verification and instrumented Android tests; Apple runtime checks remain separate."
    dependsOn("harnessVerify", "harnessDevice")
}
