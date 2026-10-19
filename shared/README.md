# Base compartilhada do SobraAi

`:shared` é uma biblioteca Kotlin Multiplatform consumida pelo host Android
`:app` e pelo host iOS via `:sharedUi`. Compartilha regras, DTOs, transporte
HTTP JSON, repositórios e lógica de sessão. Não contém interface ou dependências
Android/JVM em `commonMain`; engines e persistência nativas ficam nos source sets
por plataforma. Detalhes: [API e sessão](../docs/kmp-api-session.md).

O host iOS inicial agora existe em `iosApp/` e consome `:shared` pela interface
Compose de `:sharedUi`. Login, Início, Rendas/Despesas, configuração Xcode e limites
de validação estão em [Compose iOS](../docs/kmp-compose-ios.md).

## Configuração

- Kotlin/KMP e compilador Compose do Android: 2.4.21.
- AGP e plugin `com.android.kotlin.multiplatform.library`: 9.2.0.
- Gradle Wrapper: 9.4.1; daemon JDK 21; bytecode Android JVM 11.
- Android: namespace `br.com.sobraai.shared`, compileSdk 36, minSdk 24.
- Apple: `iosArm64` e `iosSimulatorArm64`, com framework estático `SobraAiShared`.
- Rede: Ktor 3.2.3 (OkHttp 4.12 no Android, Darwin no iOS), serialization 1.11.0.
  A versão Ktor preserva compatibilidade com o compileSdk Android existente.
- As bibliotecas de interface Android e os arquivos de UI foram preservados.

A combinação está na [matriz oficial de compatibilidade KMP](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html).
A referência de Xcode dessa linha Kotlin é 26.4; confirmar também o macOS
compatível ao preparar o ambiente Apple. Isso não significa que houve build Apple.

## Código e testes

`src/commonMain/kotlin/com/example/appfinanceiro/` contém `core/date/MonthNavigation.kt`
e `feature/despesas/ExpenseFormValidator.kt`. Os packages, assinaturas e mensagens
foram preservados; as telas Android usam as mesmas funções sem adaptações.
Operações de mês usam divisão de piso e módulo Kotlin, inclusive para negativos.
Não manter cópias desses arquivos em `:app`. `core/network/` e `core/data/` também
contêm os contratos e implementações compartilhadas de API e sessão.

`src/commonTest/kotlin/` usa `kotlin.test`. Navegação mensal cobre virada de ano,
deslocamentos negativos/múltiplos anos, zero, índices inválidos e deslocamentos
extremos. Formulário cobre campos obrigatórios, precedência e limite de notas.
A migração não acrescenta trim ou interpretação monetária ao validador existente.
API e sessão acrescentam testes de contrato HTTP com MockEngine, serialização
legada, erros/cancelamento e persistência segura fake. `src/androidHostTest/`
também testa compatibilidade com as chaves antigas em DataStore temporário.

Na raiz, em Windows:

```powershell
.\gradlew.bat harnessUnit
.\gradlew.bat harnessVerify :app:assembleDebug
```

As regras comuns são executadas via `:shared:testAndroidHostTest`, na JVM.
Relatório: `shared/build/reports/tests/testAndroidHostTest/index.html`.
Para investigação dirigida: `:shared:testAndroidHostTest --tests "com.example.appfinanceiro.core.date.MonthNavigationTest"`.

## Validação Apple pendente

Os targets permanecem declarados no Windows, mas os testes de simulador são
desabilitados pelo Gradle nesse host. Não ocultar esse aviso. Nenhum sucesso JVM
substitui compilação/link ou teste Native.

Kotlin 2.4.21 também permite compilar estes fontes para KLIB Apple no Windows.
Isso valida os tipos e APIs usados, sem gerar um framework final ou executar
Keychain/Darwin. Para essa compilação sem execução:

```powershell
.\gradlew.bat :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
.\gradlew.bat :shared:compileTestKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64
```

No Mac com Xcode configurado, executar a partir da raiz:

```sh
./gradlew :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:linkReleaseFrameworkIosArm64 :shared:linkReleaseFrameworkIosSimulatorArm64
```

Quando o usuário tiver preparado e autorizado o simulador Apple:

```sh
./gradlew :shared:iosSimulatorArm64Test
```

Os nomes de tarefas foram conferidos na configuração Gradle; sua execução Apple
continua pendente. Resultados dos frameworks ficam em `shared/build/bin/`.

O projeto `iosApp/SobraAi.xcodeproj` integra o framework de entrada
`SobraAiUI`, produzido por `:sharedUi:embedAndSignAppleFrameworkForXcode`.
Essa tarefa depende das variáveis fornecidas pelo Xcode; não é um comando
independente de compilação. `SobraAiShared` continua disponível para consumidores
sem interface. Assinatura/equipe Apple precisam de configuração local.
[Integração direta oficial](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html).

O host iOS de login/Início usa Compose Multiplatform em `:sharedUi`, que depende
de `:shared`, com deployment target iOS 15. Comandos e validação pendente em
[Compose iOS](../docs/kmp-compose-ios.md). O Android mantém sua identidade e proteções.

## Validação da fundação em 19/10/2026

- `harnessUnit`: 44 testes do app e 10 testes comuns na JVM, sem falhas.
- `harnessVerify :app:assembleDebug`: concluído com sucesso; reutilizou os
  resultados unitários da execução anterior e executou Lint e montagem debug.
- Comparação SHA-256: 113 fontes/recursos Android preservados, excluídos apenas
  os dois arquivos de regras movidos para `:shared`.
- Nenhum emulador iniciado, aplicativo instalado/aberto ou teste instrumentado
  executado. Interface em execução e validação Apple continuam pendentes.

## Validação da etapa de API e sessão

Em 19/10/2026, a rodada final passou com 44 testes JVM do app, 30 do módulo
compartilhado, Lint debug e build debug. Fontes e testes Kotlin/Native dos dois
targets Apple também compilaram para KLIB; os testes Native não foram executados
e o framework final não foi linkado. Detalhes, checks adicionais e limitações
estão em [API e sessão](../docs/kmp-api-session.md#resultado-em-19102026).
