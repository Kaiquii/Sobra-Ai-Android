# Orientações para agentes — SobraAi KMP (Android e iOS)

## Comece aqui

Este repositório contém o SobraAi, um projeto **Kotlin Multiplatform para Android
e iPhone**. Toda mudança deve preservar o funcionamento nas duas plataformas.
O host Android `:app` mantém seu front-end em Jetpack Compose; `:shared` contém
regras, API e sessão comuns. `:sharedUi` contém as telas Compose Multiplatform,
hospedadas por `iosApp/SobraAi.xcodeproj`; `:app` não depende de `:sharedUi`.
Leia [a política KMP](rules.md) e consulte `docs/kmp-compose-ios.md` para a
integração Apple. Sucesso Android/JVM não comprova funcionamento no iPhone.
O backend é externo: não presumir Go, Docker ou acesso ao
repositório do servidor a partir de informações de outra conversa.

Antes de editar:

1. Confira `git status --short` e preserve alterações existentes.
2. Leia [o mapa do projeto](docs/ai-project-map.md) e localize o fluxo afetado.
3. Leia [o fluxo de trabalho e validação](docs/ai-workflow.md).
4. Para mudanças visuais, leia integralmente [as regras de front-end](docs/frontend-rules.md).
5. Inspecione o código e os testes relevantes; a documentação é um mapa, não uma
   substituição da implementação. Atualize o mapa quando mudar a arquitetura.
6. Confira [rules.md](rules.md), identifique o impacto em Android e iOS e escolha
   as verificações adequadas para o código comum e os adaptadores nativos.

Responda em português brasileiro. Execute o trabalho solicitado até a validação
adequada; se houver impedimento real, informe o que falta e a evidência. Não
declare testes, interface ou release aprovados sem ter feito essa validação.

## Identidade e organização

- `namespace` e `applicationId` do Android `:app`: `br.com.sobraai.app`.
- Base dos fontes Android: `app/src/main/java/com/example/appfinanceiro`.
- O pacote Kotlin predominante é `com.example.appfinanceiro`; `R` e `BuildConfig`
  são gerados em `br.com.sobraai.app`. Não fazer renomeação global para igualá-los.
- `MainActivity.kt` concentra a navegação Compose e a entrada de sessão.
- `feature/` contém telas, componentes específicos e ViewModels.
- `core/data/` contém repositórios, contratos de fontes de dados e tratamento de erros.
- `shared/.../core/network/` contém DTOs, contratos e transporte JSON Ktor comum.
  `app/.../core/network/` mantém configuração Android e Retrofit para foto/exportação.
- `core/designsystem/` contém tema e componentes reutilizáveis.
- `shared/src/commonMain/kotlin/` contém regras compartilhadas com iOS; os pacotes
  existentes foram preservados. Não duplicar essas regras dentro de `:app`.
- `sharedUi/src/commonMain/` contém telas/tema/apresentadores comuns. Adaptadores
  UIKit/Speech ficam em `sharedUi/src/iosMain/`; o host e a configuração Xcode
  ficam em `iosApp/`. Não mover APIs nativas para `commonMain`.
- A sessão usa `SessionStore`/`PersistentSessionStore` comuns. `SessionManager`
  está em `shared/src/androidMain/` e preserva o DataStore `user_session` e suas
  chaves. `IosSessionStore` usa Keychain e preferências Apple; validar no Mac/iPhone.
- Algumas pastas não coincidem com o `package`: conferir a declaração antes de importar.

## Regras de implementação

- Trate Android e iOS como plataformas mantidas do projeto. Priorize código
  comum compatível e preserve os adaptadores e consumidores das duas plataformas.
  Não introduza dependências Android/JVM em `commonMain` nem remova targets para
  fazer um build passar. Não altere o front-end Android como efeito da migração.
- Siga o padrão existente da funcionalidade. Para lógica nova, prefira ViewModels,
  funções testáveis e repositórios; não coloque chamadas HTTP na recomposição.
- Preserve os contratos de `FinanceDataSources.kt` e a possibilidade de injetar
  fakes nos ViewModels. Não introduza framework de DI ou dependências sem necessidade.
- Reutilize os componentes e tokens de tema existentes. Textos de interface são
  em português brasileiro, com acentos e arquivos UTF-8.
- Preserve nomes JSON, endpoints, parâmetros, tipos e valores de domínio da API.
  Verifique todos os consumidores ao alterar um DTO.
- Em regras financeiras, verifique parcelas, recorrência, adiantamentos, rateio,
  status de pagamento, arredondamento e filtros de mês/ano aplicáveis. Não mude
  unidade monetária, tipo numérico ou significado de saldo incidentalmente.
- Preserve propagação de cancelamento de coroutines e os estados de sessão/erro.
  Dados antigos não devem parecer pertencer a um filtro novo.
- Não exponha tokens, senhas, dados financeiros pessoais ou valores de configuração
  em logs, documentação, fixtures ou commits. Use dados sintéticos nos testes.
- Mantenha HTTPS, biometria, Play Integrity e regras de segurança de release.
  Não desative proteção para fazer um teste passar.
- Não edite saídas em `build/`, caches ou APK/AAB como se fossem fontes. Não altere
  versão, assinatura ou publique uma release como consequência de uma tarefa comum.

## Validação rápida

O usuário prepara o emulador/simulador e realiza os testes visuais/manuais.
Não iniciar emuladores/simuladores, instalar ou abrir o app, nem executar testes
instrumentados por iniciativa própria. Só realizar essas ações se o usuário
solicitar explicitamente.
Por padrão, validar com build Android, Lint, testes JVM e compilação KMP para iOS;
informar o que ficou para o usuário conferir nas duas plataformas. É permitido
compilar os testes sem executar emuladores/simuladores ou dispositivos.

Execute os comandos a partir da raiz, com o Gradle Wrapper:

```powershell
.\gradlew.bat harnessUnit
.\gradlew.bat harnessVerify
```

O primeiro é parcial: testes JVM de `:app`, `:shared` e `:sharedUi`. O segundo é
a verificação local padrão: acrescenta APK Android debug, Lint e fontes/testes
KLIB de `shared`/`sharedUi` para `iosArm64` e `iosSimulatorArm64`.
O script `scripts/test-harness.ps1` usa `verify` por padrão. No Mac, complemente
com `harnessIos` e build Xcode; testes Native (`harnessIosTest`) exigem solicitação
explícita e simulador preparado. Sucesso no Windows não comprova execução Apple.
Para dispositivo, release, configuração do ambiente e critérios de conclusão, siga
[o fluxo de validação](docs/ai-workflow.md). O [harness de testes](docs/test-harness.md)
é uma ferramenta de apoio a estas instruções.

Mudanças apenas documentais precisam de revisão de conteúdo, caminhos e
`git diff --check`, não de recompilar todo o aplicativo.
