# Interface compartilhada com Compose Multiplatform

## Implementação

`:sharedUi` contém telas, tema e apresentadores Kotlin comuns em
`sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/`. Compila para Android,
iPhone ARM64 e simulador Apple Silicon. Usa Compose Multiplatform 1.10.0,
Material 3 1.9.0 e o compilador Compose Kotlin 2.4.21 do projeto.
O módulo `:app` **não depende de `:sharedUi`**: suas telas, recursos, navegação,
tema e dependências Compose permanecem intactos.

`iosApp/SobraAi.xcodeproj` hospeda um `ComposeUIViewController` em SwiftUI.
As telas são Kotlin; não há interface financeira duplicada em Swift.
`IosApp` cria uma única sessão Keychain e um cliente Ktor/Darwin por host.
O host cobre a janela inativa para proteger os dados no seletor de aplicativos.

Esta etapa oferece:

- Login com e-mail/senha, validação, carregamento, erros e bloqueio de cinco
  minutos por e-mail após HTTP 429. Token e identidade ficam no Keychain;
  a senha não entra no estado Compose restaurável.
- Oferta de Face ID/Touch ID após o login. Ativação exige autenticação real
  pelo `LocalAuthentication`. Sessões protegidas não abrem o painel antes de
  autenticar. Cancelamento mantém o bloqueio; entrar com senha encerra a
  sessão anterior. Segundo plano bloqueia novamente quando a opção está ativa.
- Início com resumo, rendas, saldos restantes, totais, seleção mensal,
  atualização, despesas do período efetivo, filtros combinados de categoria,
  origem e status, detalhes com parcelas/rateios/adiantamentos, ocultação de
  valores e saída. Status é filtrado localmente na mesma resposta de despesas
  efetivas; origem considera rateios. Totais e saldos do servidor são preservados;
  rendas reconhecem os mesmos nomes de domínio do Android.
- Snapshot atômico por período: uma resposta antiga não sobrescreve o mês novo.
  Falhas mantêm os dados anteriores com seu mês identificado, inclusive durante
  nova tentativa. Expiração/revogação limpam o painel e encerram a sessão.
  Falhas no armazenamento seguro não simulam logout concluído.
- Tema claro/escuro com os tokens existentes, rolagem, teclado, áreas seguras e
  alvos de toque. Valores ocultos mantêm o espaço e saem das semânticas de leitura.

O escopo agora inclui login, Início, criação/edição/exclusão de rendas e despesas,
pagamento, adiantamentos e criação de categoria no formulário. A navegação comum
e os contratos estão detalhados em [rendas e despesas](kmp-rendas-despesas.md).
Assistente, relatórios e perfil foram acrescentados na
[etapa de funcionalidades finais](kmp-funcionalidades-ios.md), incluindo arquivos,
foto, categorias, ajuda e ditado. Cadastro e recuperação têm formulários e
confirmação de códigos; o **envio** aguarda o protocolo Apple de proteção do
backend, registrado em [pendências do backend](pendencias-backend-ios.md).
Não há substituição de Play Integrity nem provas fictícias.
Não há afirmação de paridade visual pixel a pixel ou aprovação visual sem aparelho.

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

Na raiz, com a configuração habitual da API:

```powershell
.\gradlew.bat harnessVerify :app:assembleDebug
.\gradlew.bat :sharedUi:compileKotlinIosArm64 :sharedUi:compileKotlinIosSimulatorArm64
.\gradlew.bat :sharedUi:compileTestKotlinIosArm64 :sharedUi:compileTestKotlinIosSimulatorArm64
```

O harness inclui `:sharedUi:testAndroidHostTest`. Os testes cobrem sessão segura,
login/logout, erros de armazenamento, biometria, rate limit, cancelamento,
respostas fora de ordem, filtros de rateio/status, períodos e saldos/formatação.
Usam fakes e não acessam backend, Keychain ou sensores reais.

No Mac, execute também:

```sh
./gradlew :sharedUi:iosSimulatorArm64Test
xcodebuild -project iosApp/SobraAi.xcodeproj -scheme SobraAi \
  -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

Gradle usa a configuração API_BASE_URL/local.properties existente; o Xcode usa
Local.xcconfig. Compilar KLIB no Windows **não** compila Swift, não faz link do
framework e não testa UIKit, Darwin ou Keychain. Essas verificações exigem Mac/Xcode.
Conferir em iPhone: login inválido/válido/429, restauração, oferta/cancelamento e
bloqueio Face ID, segundo plano, sessão expirada, logout, meses rápidos, rede e
nova tentativa, filtros, parcelas/rateios, privacidade/VoiceOver, temas, rotação,
fonte ampliada e teclado em tela pequena. O usuário conduz o teste em dispositivo.

Referências: [release Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.10.0)
e [integração UIKit](https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html).

## Evidência da primeira interface — 19/10/2026

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

A etapa posterior de edição tem suas evidências e cenários em
[kmp-rendas-despesas.md](kmp-rendas-despesas.md); os números acima são históricos.
