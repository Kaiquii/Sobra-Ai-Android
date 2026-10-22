# 💰 SobraAi

## Orientações para IA

O ponto de entrada para agentes é [AGENTS.md](AGENTS.md), com regras do projeto,
[política KMP para Android e iPhone](rules.md),
[mapa da arquitetura](docs/ai-project-map.md) e
[fluxo de desenvolvimento e validação](docs/ai-workflow.md).
Essas instruções orientam a IA ao trabalhar no código e usam o harness de testes
documentado abaixo como ferramenta de validação.

Aplicativo de gestão financeira em Kotlin Multiplatform, integrado a uma API
REST externa. Android e iOS usam **as mesmas telas, navegação e apresentação
Compose Multiplatform**, preservando o visual completo do Android. Ambos os hosts
abrem `SobraAiApp(services)`; telas iOS simplificadas ou paralelas são proibidas.

| Módulo | Responsabilidade |
| --- | --- |
| `app` | Host Android e integrações nativas; depende de `sharedUi` |
| `shared` | Regras, contratos HTTP, repositórios e sessão compartilhados |
| `sharedUi` | Todas as telas, navegação e apresentação comuns Android/iOS |
| `designSystem` | Componentes e tema Compose comuns |
| `iosApp` | Host SwiftUI/UIKit e projeto Xcode do iOS |

## 🚀 Funcionalidades

- **Autenticação Completa:** Login, Registo de novos utilizadores e Recuperação/Alteração de Senha.
- **Integração com API:** Comunicação assíncrona e segura com o back-end.
- **UI/UX Moderna:** Interface construída 100% com Jetpack Compose, priorizando a fluidez e um design de alto contraste.
- **Gestão de Estado:** Navegação única entre telas comuns nas duas plataformas.

## 🛠️ Tecnologias Utilizadas

O projeto foi construído utilizando as ferramentas mais modernas do ecossistema Android:

- **[Kotlin](https://kotlinlang.org/):** Linguagem de programação oficial para desenvolvimento Android.
- **[Jetpack Compose](https://developer.android.com/jetpack/compose):** Toolkit moderno para construção de UI nativa de forma declarativa.
- **Kotlin Multiplatform e Compose Multiplatform:** Código, telas e apresentação usados pelos hosts Android e iOS.
- **Ktor e kotlinx.serialization:** Transporte HTTP e serialização da camada compartilhada.
- **[Retrofit 2](https://square.github.io/retrofit/):** Cliente HTTP type-safe para consumo da API REST.
- **[Gson](https://github.com/google/gson):** Serialização e desserialização de JSON.
- **Coroutines:** Gerenciamento de tarefas em segundo plano e chamadas de rede assíncronas.
- **Apresentação comum:** Navegação e estados em `sharedUi/commonMain`, com serviços nativos injetados.

## ⚙️ Como executar o projeto localmente

Para desenvolver o Android, use o **Android Studio**. Para o iOS, siga a seção
[Build iOS no Mac](#build-ios-no-mac) após obter o código.

### 1. Clonar o repositório

```bash
git clone https://github.com/Kaiquii/Sobra-Ai-Android.git
cd Sobra-Ai-Android
```

## Build iOS no Mac

Execute os comandos abaixo **na raiz do repositório**. O projeto contém targets
`iosArm64` (iPhone/iPad) e `iosSimulatorArm64` (simulador em Mac Apple Silicon).
Não há target de simulador Intel `iosX64` configurado.

### 1. Preparar o ambiente

- Mac Apple Silicon para seguir o fluxo de simulador ARM64 deste guia.
- **Xcode 26.4**, referência da [matriz oficial para Kotlin 2.4.21](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html),
  com macOS compatível. Abra o Xcode uma vez, aceite os termos e instale os
  componentes solicitados e o runtime do simulador iOS.
- **JDK 21** instalado. O Wrapper usa Gradle 9.4.1 e o daemon está configurado
  para Java 21; o bytecode Android continua em JVM 11.
- **Android SDK**, mesmo para o build Apple: o Gradle também configura os
  módulos Android. No SDK Manager do Android Studio, instale as plataformas
  **Android 36 e 36.1**, Build-Tools compatíveis e Platform-Tools.
- Acesso à internet no primeiro build para obter dependências Gradle/Kotlin.
  Para executar em iPhone físico, configure também sua conta/equipe Apple.

No Terminal, selecione o JDK e confira as ferramentas:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
xcode-select -p
xcodebuild -version
/bin/sh ./gradlew --version
```

Se `xcode-select -p` apontar apenas para Command Line Tools, selecione a instalação
completa do Xcode (ajuste o caminho se estiver em outro local):

```sh
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

### 2. Configurar SDK, API e assinatura

Crie ou ajuste `local.properties` na raiz com o caminho do SDK **no Mac** e a
URL HTTPS real da API. Preserve outras configurações locais que já existirem:

```properties
sdk.dir=/Users/SEU_USUARIO/Library/Android/sdk
API_BASE_URL=https://example.invalid/
```

O caminho do SDK pode ser conferido no Android Studio. `example.invalid` é apenas
um exemplo e deve ser substituído pela API real. Não reutilize o caminho Windows
de `sdk.dir`. `local.properties` é ignorado pelo Git.

Crie a configuração local do Xcode uma única vez:

```sh
cp -n iosApp/Configuration/Local.xcconfig.example iosApp/Configuration/Local.xcconfig
```

Edite `iosApp/Configuration/Local.xcconfig`:

```xcconfig
API_BASE_URL = https:/$()/example.invalid/
// Necessário para assinatura em aparelho físico:
// DEVELOPMENT_TEAM = SEU_TEAM_ID
// Ajuste se sua equipe precisar de outro identificador:
// PRODUCT_BUNDLE_IDENTIFIER = br.com.suaequipe.sobraai
```

Use a mesma API real nas duas configurações. No `.xcconfig`, preserve
`https:/$()/`: a expansão `$()` evita que `//` seja interpretado como comentário.
Esse arquivo também é ignorado pelo Git. Para simulador sem assinatura, a equipe
pode ficar sem configuração; para iPhone, informe seu Team ID ou selecione a
equipe em **Signing & Capabilities** no Xcode. Alterar o bundle identifier muda
o serviço de sessão do Keychain e exige novo login.

### 3. Compilar e executar pelo Xcode

```sh
open iosApp/SobraAi.xcodeproj
```

1. Selecione o scheme **SobraAi**.
2. Escolha um simulador de iPhone ARM64 instalado ou seu iPhone conectado.
   O deployment target do projeto está configurado para iOS 15.0.
3. Para aparelho físico, confira a equipe e o provisionamento em
   **Signing & Capabilities**.
4. Use **Product > Build** (`⌘B`) para compilar ou **Product > Run** (`⌘R`)
   para compilar e executar no destino escolhido.

A fase **Compilar interface Kotlin** já chama `iosApp/build-framework.sh`, que
executa `:sharedUi:embedAndSignAppleFrameworkForXcode` com a API configurada.
O Xcode fornece SDK, arquitetura e configuração à tarefa. Não é necessário
executar essa tarefa isoladamente, instalar CocoaPods ou gerar outro projeto.
O primeiro build pode demorar pelos downloads e pela compilação Kotlin/Native.

### 4. Compilar o simulador pelo Terminal

Depois da configuração acima, este comando gera o app sem abri-lo nem instalá-lo:

```sh
xcodebuild \
  -project iosApp/SobraAi.xcodeproj \
  -scheme SobraAi \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath iosApp/build/DerivedData \
  ARCHS=arm64 \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Saída esperada: `iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/SobraAi.app`.
Esse artefato é para simulador; não é um IPA para instalar no iPhone.

### 5. Validar

Com SDK e API configurados, execute a verificação local Android/KMP pela raiz:

```sh
/bin/sh ./gradlew harnessVerify
```

Esse grupo inclui testes JVM, APK/Lint Android e compilação dos fontes/testes
KLIB de `shared`/`sharedUi` para aparelho e simulador iOS ARM64. Para fazer também
o link dos frameworks debug no Mac, sem iniciar o simulador:

```sh
/bin/sh ./gradlew harnessIos
```

Para executar também os testes Kotlin/Native no simulador preparado no Mac:

```sh
/bin/sh ./gradlew harnessIosTest
```

O build Xcode valida Swift e o link do framework. Depois, confira no iPhone
login/sessão, Face ID ou Touch ID, segundo plano, rendas/despesas, relatórios e
exportação, assistente/voz, perfil/fotos e permissões. Compilar ou passar nos
testes JVM não substitui essa execução. Cadastro/recuperação ainda dependem do
[backend Apple e seu adaptador nativo](docs/pendencias-backend-ios.md); essa
pendência não é resolvida pelo build.

### Problemas comuns

| Mensagem ou sintoma | O que conferir |
| --- | --- |
| Java não encontrado no Terminal/Xcode | JDK 21 instalado e reconhecido por `/usr/libexec/java_home -v 21`. Se necessário, defina `JAVA_HOME` no `Local.xcconfig` com o caminho retornado, sem aspas |
| SDK Android ausente | `sdk.dir` do Mac em `local.properties` e plataformas 36/36.1 instaladas |
| API não configurada | `API_BASE_URL` em `local.properties` para Gradle manual e em `Local.xcconfig` para o host Xcode |
| Nenhum simulador disponível | Runtime iOS instalado no Xcode e destino ARM64 selecionado |
| Erro de assinatura no iPhone | Conta/equipe, bundle identifier e provisionamento do target SobraAi |

Mais detalhes: [integração Compose/iOS](docs/kmp-compose-ios.md),
[funcionalidades e evidências](docs/kmp-funcionalidades-ios.md) e
[pendências do backend](docs/pendencias-backend-ios.md).
O build e a execução Apple ainda precisam ser realizados no Mac; este guia não
declara que já foram aprovados. O front-end Android permanece preservado.

## 🧪 Harness de testes

O harness considera **Android e iPhone** em toda mudança. O modo padrão `verify`
executa testes JVM dos três módulos, gera o APK Android debug, roda Lint e compila
os fontes/testes Kotlin para os dois targets iOS, sem iniciar dispositivos:

```powershell
.\scripts\test-harness.ps1
```

`unit` é uma verificação parcial; link/build Swift e execução no iPhone exigem
Mac/Xcode e evidências próprias. Consulte [a documentação do harness](docs/test-harness.md)
para modos, comandos Apple, relatórios e limites de cada verificação.
