# Interface compartilhada com Compose Multiplatform

## Implementação única

**Android e iOS usam exatamente a mesma aplicação Compose: `SobraAiApp(services)`**,
em `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/SharedApplication.kt`.
O Android `:app` depende de `:sharedUi`. `MainActivity` e `IosApp` apenas criam
os serviços nativos e encaminham o ciclo de vida. SwiftUI hospeda o
ComposeUIViewController, sem telas financeiras próprias.

Todas as telas completas e seus ViewModels foram migrados do Android para
`sharedUi/src/commonMain/kotlin/com/example/appfinanceiro/`, preservando os
packages, layouts, componentes, textos e contratos. As antigas telas iOS
simplificadas foram removidas. A navegação é comum, inclusive telas secundárias,
formulários, detalhes, modais, filtros, histórico, exportação e configurações.
Os componentes reutilizáveis e tokens ficam em `designSystem/src/commonMain/`.
A tipografia usa os mesmos TTF Roboto (400/500/600/700/800) nos recursos comuns,
sem downloads durante o uso. A origem e a licença estão em
[Roboto.md](../designSystem/third_party/Roboto.md).
O diálogo de atualização obrigatória/opcional e sua decisão também são comuns,
em `sharedUi/.../core/update/`. `AppUpdates` injeta versão e ações de loja;
nenhuma apresentação Compose permanece no host Android.

`UiServices` injeta sessão, repositórios, arquivos, biometria e serviços nativos.
Os modelos de apresentação possuem escopos cancelados ao encerrar/bloquear a
sessão; a navegação conserva modelos das abas e estado restaurável. Senhas e
códigos não são serializados em estado restaurável.

Os targets são Android, `iosArm64` e `iosSimulatorArm64`, com Compose 1.10.0,
Material 3 1.9.0 e o compilador Kotlin do projeto. A guarda
`verifySharedPresentation` impede telas/ViewModels por plataforma e duplicações.
Não basta compartilhar cards: [regra obrigatória](../rules.md#interface-única-android-e-ios-devem-ser-visualmente-iguais).

## Fronteiras nativas

- Android: DataStore, Play Integrity, BiometricPrompt, galeria, permissões,
  SpeechRecognizer, MediaStore/seleção de documento e abertura/compartilhamento.
- Apple: Keychain, LocalAuthentication, galeria UIKit, Speech, documentos e
  compartilhamento. `LAContext.biometryType` distingue Face ID de Touch ID;
  o texto e o ícone comuns usam somente o sensor detectado. Ativação exige
  autenticação real e a cena inativa fica coberta pelo host Swift.
- O ícone original Android está em `iosApp/SobraAi/Assets.xcassets/AppIcon.appiconset`,
  incluído no target Xcode com `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`.
- Envio de códigos Apple e consulta de versão da App Store dependem de contrato
  externo: [pendências do backend](pendencias-backend-ios.md). A migração de UI
  não inventa APIs, prova de integridade ou exceções de segurança.

A aprovação visual depende de comparação em Android e iPhone com os mesmos
dados, temas e configurações de fonte. KLIB/JVM não prova igualdade de pixels,
link Xcode, permissões, sensores ou funcionamento no aparelho.

## Abrir no Mac

1. Instale JDK 21, Xcode compatível com Kotlin 2.4.21 e o SDK Android usado pelo
   repositório. O Gradle configura também `:app`, mesmo ao compilar para Apple.
2. Copie `iosApp/Configuration/Local.xcconfig.example` para `Local.xcconfig` na
   mesma pasta. Configure a URL HTTPS real e `DEVELOPMENT_TEAM`. A sintaxe
   `https:/$()/` preserva barras no xcconfig. O domínio sintético do exemplo
   não permite login real. O arquivo local é ignorado no Git.
3. Abra `iosApp/SobraAi.xcodeproj`, scheme `SobraAi`, com iPhone ou simulador ARM64.
   Ajuste bundle identifier/equipe conforme sua conta. Alterar o bundle também
   muda o serviço do Keychain e exige novo login.
4. Compile no Xcode. A fase `build-framework.sh` chama o Wrapper com
   `:sharedUi:embedAndSignAppleFrameworkForXcode` e repassa a URL como propriedade
   Gradle. O SDK e a configuração vêm do Xcode. Não precisa de CocoaPods/XcodeGen.

A URL entra no Info.plist; HTTP não recebe exceção ATS. Não há equipe, certificado
ou provisionamento privados versionados. Não foi publicada uma versão App Store.

## Verificação

O projeto mantém Android e iPhone como plataformas, conforme [rules.md](../rules.md).
`harnessVerify` agora cobre testes JVM, APK/Lint Android e fontes/testes KLIB iOS
de `shared` e `sharedUi`. Esse é o modo padrão do script Windows.

Na raiz, com a configuração habitual da API:

```powershell
.\gradlew.bat harnessVerify
```

O harness inclui `:sharedUi:testAndroidHostTest`. Os testes cobrem sessão segura,
login/logout, erros de armazenamento, biometria, rate limit, cancelamento,
respostas fora de ordem, filtros de rateio/status, períodos e saldos/formatação.
Usam fakes e não acessam backend, Keychain ou sensores reais.

No Mac, complemente com link dos frameworks; testes Native exigem pedido
explícito e simulador preparado:

```sh
/bin/sh ./gradlew harnessIos
# Somente para executar os testes Native solicitados:
/bin/sh ./gradlew harnessIosTest
xcodebuild -project iosApp/SobraAi.xcodeproj -scheme SobraAi \
  -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

Alcance de cada grupo em [test-harness.md](test-harness.md).

Gradle usa a configuração API_BASE_URL/local.properties existente; o Xcode usa
Local.xcconfig. Compilar KLIB no Windows **não** compila Swift, não faz link do
framework e não testa UIKit, Darwin ou Keychain. Essas verificações exigem Mac/Xcode.
Conferir em iPhone: login inválido/válido/429, restauração, oferta/cancelamento e
bloqueio Face ID, segundo plano, sessão expirada, logout, meses rápidos, rede e
nova tentativa, filtros, parcelas/rateios, privacidade/VoiceOver, temas, rotação,
fonte ampliada e teclado em tela pequena. O usuário conduz o teste em dispositivo.

Referências: [release Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.10.0)
e [integração UIKit](https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html).

## Evidências da unificação — 22/10/2026

- `harnessVerify` concluído: **163 testes JVM**, sendo 44 em `app`, 37 em
  `shared` e 82 em `sharedUi`, sem falhas, erros ou testes ignorados.
- APK debug e Lint Android concluídos, com zero erros de Lint. As tarefas
  inalteradas puderam reutilizar resultados do Gradle.
- Fontes de `shared`, `sharedUi` e `designSystem` e testes de `shared`/`sharedUi`
  compilados para `iosArm64` e `iosSimulatorArm64`. Testes Native não executados.
- A guarda confirmou as 15 telas únicas, o diálogo de atualização comum,
  ausência de telas/ViewModels/Compose nos hosts e a entrada `SobraAiApp(services)`
  nas duas plataformas. Somente os hooks nativos de voltar/janela são exceções.
- Os cinco TTF Roboto do APK e dos pacotes de recursos dos dois targets Apple
  têm os mesmos hashes dos fontes. Licença OFL incluída no APK. Os 18 PNGs do
  AppIcon têm as dimensões esperadas e não contêm transparência.
- Links locais e `git diff --check` revisados. Nenhum dispositivo ou simulador
  iniciado pelo agente. Nenhuma instalação ou publicação realizada.
- **Pendentes:** build/link Swift/Xcode, empacotamento final Apple e comparação
  visual em Android/iPhone pelo usuário, incluindo sensores, teclado e permissões.
  O envio de códigos e a consulta de versão Apple dependem do contrato externo
  registrado em [pendencias-backend-ios.md](pendencias-backend-ios.md).

## Evidência histórica da primeira interface — 19/10/2026

- `harnessVerify :app:assembleDebug` aprovado: 44 testes do app, 30 de `shared`
  e 15 de estado/apresentação de `sharedUi`, sem falhas. Lint debug e montagem
  do APK concluídos; tarefas inalteradas puderam reaproveitar resultados.
- Fontes e testes KLIB de `sharedUi` compilados para `iosArm64` e
  `iosSimulatorArm64`. Testes Native não executados no Windows.
- Comparação SHA-256 dos 68 fontes de interface Android sem mudanças;
  recursos e Manifest Android também sem alterações.
- XML do Info.plist/scheme, referências internas do projeto Xcode, sintaxe do
  harness PowerShell e whitespace revisados. Essa inspeção não substitui Xcode.
- Nenhum emulador/app iniciado e nenhuma release gerada/publicada.
  Build Swift, link do framework, execução de UI e serviços Apple continuam
  pendentes por falta de Mac/Xcode neste ambiente.

Os números e a comparação de arquivos acima são anteriores à migração das telas
para código único. A arquitetura atual está no início deste documento e no
[mapa do projeto](ai-project-map.md). Não usar esta evidência histórica para
declarar o novo código ou a igualdade visual aprovados.
