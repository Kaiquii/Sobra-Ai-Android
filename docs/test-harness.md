# Harness Android e iOS/KMP

O SobraAi é um projeto **Kotlin Multiplatform para Android e iPhone**. O harness
deve detectar regressões no código comum e nos adaptadores das duas plataformas;
aprovação apenas Android/JVM não comprova funcionamento iOS. A política do
projeto está em [rules.md](../rules.md) e [AGENTS.md](../AGENTS.md).

Os grupos abaixo usam o Gradle Wrapper. `harnessVerify` é a verificação local
padrão e não inicia emuladores/simuladores, instala apps ou publica releases.

## Modos e alcance

| Modo PowerShell | Tarefa Gradle | Verificações | Dispositivo/ambiente |
| --- | --- | --- | --- |
| `verify` (padrão) | `harnessVerify` | Testes JVM dos três módulos, APK Android debug, Lint e `harnessKmp` | Sem dispositivo |
| `unit` | `harnessUnit` | Testes JVM de `app`, `shared` e `sharedUi`; verificação parcial | Sem dispositivo |
| `kmp` | `harnessKmp` | Compilação Android e fontes/testes KLIB para os dois targets iOS | Sem dispositivo |
| `device` | `harnessDevice` | Testes instrumentados Android; verificação parcial | Android preparado; pedido explícito |
| `all` | `harnessAll` | `harnessVerify` e instrumentados Android | Android preparado; pedido explícito |
| — | `harnessIos` | `harnessVerify` e link dos frameworks Apple debug | Mac/Xcode; sem executar simulador |
| — | `harnessIosTest` | `harnessIos` e testes Native de `shared`/`sharedUi` | Mac Apple Silicon/JDK ARM64 e simulador preparado; pedido explícito |

O nome legado `all` inclui os instrumentados **Android**. Ele não executa testes
Apple nem o host Swift. Nenhum modo sozinho certifica release ou paridade visual.

## Uso no Windows

Na raiz, com JDK 21, SDK Android e API configurados:

```powershell
.\scripts\test-harness.ps1
# Equivale a:
.\scripts\test-harness.ps1 -Mode verify
```

Para uma verificação dirigida:

```powershell
.\scripts\test-harness.ps1 -Mode unit
.\scripts\test-harness.ps1 -Mode kmp -NoDaemon
```

Somente quando o usuário solicitar e preparar o dispositivo Android:

```powershell
.\scripts\test-harness.ps1 -Mode device
.\scripts\test-harness.ps1 -Mode all
```

O script consulta o ADB nos modos `device`/`all`, mas não inicia emuladores.
Ele mostra o alcance do modo, os relatórios existentes e as verificações Apple
que não podem ser realizadas nesse host. Falhas encerram com código diferente
de zero; relatórios antigos não significam aprovação da execução que falhou.

## Configuração da API

O script aceita `-ApiBaseUrl`, `API_BASE_URL` no ambiente ou `local.properties`.
O Gradle resolve a URL nesta ordem: `-PAPI_BASE_URL`, `local.properties`, ambiente.
O parâmetro do script vira `-PAPI_BASE_URL`, sem imprimir seu valor. Use HTTPS.

```powershell
.\scripts\test-harness.ps1 -Mode verify -ApiBaseUrl "https://SUA_API/"
```

Substitua `SUA_API` pela configuração real. Para testes isolados sem backend,
`-Mode unit -ApiBaseUrl "https://example.invalid/"` usa um domínio sintético.
Não use esse domínio para validar integrações ou gerar um APK destinado ao usuário;
o modo `verify` também gera o APK debug.

## O que a compilação KMP cobre

`harnessKmp` inclui, em **cada** módulo `shared` e `sharedUi`:

- `compileAndroidMain`;
- `compileKotlinIosArm64` e `compileTestKotlinIosArm64`;
- `compileKotlinIosSimulatorArm64` e `compileTestKotlinIosSimulatorArm64`.

Isso compila o código comum, os adaptadores nativos e os testes comuns para
iPhone/iPad ARM64 e simulador Apple Silicon. A versão Kotlin do repositório
permite gerar KLIB Apple no Windows; o harness não remove targets nem ignora
falhas para adaptar o projeto ao host. KLIB é compilação de fontes, **não** link
de framework, build Swift, teste Native executado ou execução da interface iOS.

`harnessUnit` executa `:app:testDebugUnitTest`, `:shared:testAndroidHostTest` e
`:sharedUi:testAndroidHostTest` na JVM. `harnessVerify` combina esses testes com
`harnessKmp`, `:app:lintDebug` e `:app:assembleDebug`.

## Uso direto pelo Gradle e no Mac

No Windows:

```powershell
.\gradlew.bat harnessVerify
.\gradlew.bat harnessKmp
```

No Mac, com o ambiente do [README](../README.md#build-ios-no-mac) preparado:

```sh
/bin/sh ./gradlew harnessVerify
/bin/sh ./gradlew harnessIos
```

`harnessIos` liga os frameworks **debug** de `shared` e `sharedUi` para aparelho
e simulador ARM64. Não altera assinatura nem gera uma release. O build do host
Swift é uma etapa adicional: abra o projeto Xcode ou use o comando `xcodebuild`
do README. A fase Xcode já chama o Wrapper para integrar o framework correto.

Quando os testes de simulador forem explicitamente solicitados:

```sh
/bin/sh ./gradlew harnessIosTest
```

Esse modo pode iniciar o simulador para executar os testes Kotlin/Native; ele
não testa as telas SwiftUI/Compose nem os sensores reais. Não o execute por
iniciativa própria. No Windows, `harnessIos`/`harnessIosTest` falham explicitamente
com o requisito Mac/Xcode, sem apresentar tarefas Native desabilitadas como sucesso.
`harnessIosTest` também recusa host/JDK Intel: não há target `iosX64` no projeto.

## Relatórios e artefatos

- JVM app: `app/build/reports/tests/testDebugUnitTest/index.html`.
- JVM regras comuns: `shared/build/reports/tests/testAndroidHostTest/index.html`.
- JVM interface comum: `sharedUi/build/reports/tests/testAndroidHostTest/index.html`.
- Lint: `app/build/reports/lint-results-debug.html`.
- APK debug: `app/build/outputs/apk/debug/`.
- Instrumentados Android: `app/build/reports/androidTests/connected/debug/index.html`.
- Native (Mac): `shared/build/reports/tests/iosSimulatorArm64Test/index.html` e
  `sharedUi/build/reports/tests/iosSimulatorArm64Test/index.html`.
- Frameworks/KLIB: `shared/build/bin/` e `sharedUi/build/bin/`; a integração do
  Xcode usa também `sharedUi/build/xcode-frameworks/`.

Os testes usam fakes/MockEngine e dados sintéticos. Não fazem login no backend
real nem comprovam App Attest, Keychain, Face ID, microfone ou galeria em execução.

## Critério de entrega

Em toda alteração de código/configuração, mantenha Android e iPhone funcionais
e registre separadamente: testes JVM, APK/Lint Android, compilação KMP,
link/build Apple e execução em dispositivo. `UP-TO-DATE` reaproveita resultados;
`SKIPPED`, testes desabilitados e `--dry-run` não são testes aprovados.

Sem Mac/iPhone, conclua `harnessVerify` e declare as verificações Apple pendentes.
Não contorne proteções ou dependências externas para obter um resultado verde.
As pendências de backend estão [documentadas](pendencias-backend-ios.md).
Mudanças só documentais requerem revisão, links e `git diff --check`.
