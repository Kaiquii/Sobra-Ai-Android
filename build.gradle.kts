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
    description = "Compiles shared/sharedUi/designSystem for Android and iOS, including shared/sharedUi Apple test source sets."
    dependsOn(":designSystem:compileAndroidMain", ":designSystem:compileKotlinIosArm64", ":designSystem:compileKotlinIosSimulatorArm64")
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
    dependsOn("verifySharedPresentation", "harnessUnit", "harnessKmp", ":app:lintDebug", ":app:assembleDebug")
    doLast {
        logger.lifecycle("[harness] Verificacao local Android/KMP concluida. Build Swift/Xcode e execucao em iPhone exigem validacao Apple separada.")
        logger.lifecycle("[harness] Conferir revisao transversal: consumidores, fluxos semelhantes e regressoes afetadas. Build verde nao comprova revisao de todas as telas.")
    }
}

tasks.register("verifySharedPresentation") {
    group = "verification"
    description = "Rejects platform-specific or duplicate application screens and presentation models."
    doLast {
        val sharedRoots = listOf("sharedUi/src/commonMain/kotlin", "designSystem/src/commonMain/kotlin")
        val commonFiles = sharedRoots.flatMap { fileTree(it) { include("**/*.kt") }.files }
        val facades = commonFiles.groupBy { source ->
            Regex("(?m)^package\\s+([^\\r\\n]+)").find(source.readText())?.groupValues?.get(1)?.trim() + "/" + source.nameWithoutExtension
        }
        check(facades.none { it.value.size > 1 }) { "Arquivos comuns geram a mesma classe JVM: ${facades.filterValues { it.size > 1 }.keys}" }
        val platformFiles = listOf("app/src/main/java", "sharedUi/src/androidMain/kotlin", "sharedUi/src/iosMain/kotlin")
            .flatMap { fileTree(it) { include("**/*.kt") }.files }
        val screenDeclaration = Regex("\\bfun\\s+(\\w+Screen)\\s*\\(")
        val screens = commonFiles.flatMap { source -> screenDeclaration.findAll(source.readText()).map { it.groupValues[1] to source }.toList() }
        val expected = listOf("LoginScreen", "RegisterScreen", "EsqueciSenhaScreen", "HomeScreen", "DespesasScreen",
            "NovaDespesaScreen", "EditarDespesaScreen", "RelatoriosScreen", "InstallmentCommitmentsScreen", "AssistantScreen",
            "PerfilScreen", "ConfiguracoesRendaScreen", "CategoriasScreen", "EditarPerfilScreen", "AjudaScreen")
        expected.forEach { name -> check(screens.count { it.first == name } == 1) { "A tela $name deve ter exatamente uma implementação comum." } }
        check(screens.groupBy { it.first }.none { it.value.size > 1 }) { "Há telas duplicadas na apresentação comum." }
        platformFiles.forEach { source ->
            val code = source.readText()
            val nativeHook = source.name == "PlatformBackHandler.kt" || source.name == "WindowAppearance.kt"
            check(nativeHook || !code.contains("@Composable")) { "Apresentação Compose fora de commonMain: ${source.relativeTo(rootDir)}" }
            check(!screenDeclaration.containsMatchIn(code)) { "Tela fora de commonMain: ${source.relativeTo(rootDir)}" }
            check(!Regex("\\bclass\\s+\\w*ViewModel\\b").containsMatchIn(code)) { "ViewModel fora de commonMain: ${source.relativeTo(rootDir)}" }
        }
        check(commonFiles.count { it.name == "AppUpdateGate.kt" } == 1) { "O diálogo de atualização também deve ser comum." }
        listOf("regular", "medium", "semibold", "bold", "extrabold").forEach { weight ->
            check(file("designSystem/src/commonMain/composeResources/font/roboto_$weight.ttf").length() > 0) { "Fonte comum ausente: Roboto $weight" }
        }
        commonFiles.forEach { source -> check(!Regex("(?m)^import (android\\.|java\\.|javax\\.|platform\\.|retrofit2\\.|okhttp3\\.)").containsMatchIn(source.readText())) {
            "Dependência nativa na apresentação comum: ${source.relativeTo(rootDir)}"
        } }
        listOf("app/src/main/java/com/example/appfinanceiro/MainActivity.kt", "sharedUi/src/iosMain/kotlin/br/com/sobraai/ui/IosApp.kt")
            .forEach { check(file(it).readText().contains("SobraAiApp(services)")) { "O host $it deve abrir a mesma aplicação Compose." } }
        check(file("app/build.gradle.kts").readText().contains("implementation(project(\":sharedUi\"))")) { "O host Android precisa consumir :sharedUi." }
        logger.lifecycle("[harness] 15 telas únicas em commonMain; Android/iOS usam SobraAiApp(services), sem telas ou ViewModels nativos paralelos.")
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
