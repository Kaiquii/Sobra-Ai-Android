# Mapa do projeto para IA

O SobraAi mantém Android e iPhone com **uma única interface e apresentação Compose
Multiplatform**. A referência visual é o Android anterior à migração. Leia
[rules.md](../rules.md) e [o fluxo de validação](ai-workflow.md).

## Entradas e módulos

| Área | Caminho desde a raiz | Responsabilidade |
| --- | --- | --- |
| Aplicação Compose única | `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/SharedApplication.kt` | `SobraAiApp(services)`, sessão, navegação de todas as telas, estado restaurável e mensagens |
| Telas e apresentação | `sharedUi/src/commonMain/kotlin/com/example/appfinanceiro/` | Telas completas, diálogos, formulários e ViewModels migrados do Android, preservando os packages |
| Componentes comuns | `designSystem/src/commonMain/kotlin/com/example/appfinanceiro/` | Tema, cores, privacidade, barra inferior, cards, filtros, gráficos, perfil, ajuda e chat; Roboto embutida em `designSystem/src/commonMain/composeResources/` |
| Serviços da interface | `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/runtime/` | `UiServices`, contratos de arquivos, escopos de apresentação e formatação/data com adaptadores |
| Host Android | `app/src/main/java/com/example/appfinanceiro/MainActivity.kt` | Splash, proteção, injeção e ciclo de vida; abre `SobraAiApp(services)` |
| Adaptadores Android | `app/src/main/java/com/example/appfinanceiro/platform/` | Biometria, galeria, permissões, voz e arquivos do sistema |
| Host Apple Kotlin | `sharedUi/src/iosMain/kotlin/br/com/sobraai/ui/IosApp.kt` | Keychain, clientes, injeção, LocalAuthentication e ciclo de vida; abre o mesmo `SobraAiApp(services)` |
| Adaptadores Apple | `sharedUi/src/iosMain/kotlin/br/com/sobraai/ui/` | UIKit, Speech, datas, imagens e arquivos; nenhuma tela alternativa |
| Host Xcode | `iosApp/SobraAi.xcodeproj`, `iosApp/SobraAi/` | Hospeda ComposeUIViewController, protege a cena inativa, configura permissões e AppIcon |
| Domínio e API | `shared/src/commonMain/kotlin/com/example/appfinanceiro/` | DTOs, JSON, Ktor, contratos, repositórios, regras e sessão comuns |
| Sessão Android | `shared/src/androidMain/` | `SessionManager`, DataStore `user_session` e chaves existentes |
| Sessão Apple | `shared/src/iosMain/` | `IosSessionStore`, Keychain e preferências Apple |
| Proteções Android | `app/src/main/java/com/example/appfinanceiro/core/security/`, `platform/AndroidAppUpdates.kt` | Play Integrity, consulta de versão e abertura da loja; não transferir contratos de loja para iOS |
| Atualização e tema comuns | `sharedUi/src/commonMain/kotlin/com/example/appfinanceiro/core/update/`, `core/designsystem/theme/` | Mesma decisão e diálogo de atualização, mesmo tema; janela do sistema em adaptadores `androidMain`/`iosMain` |
| Exportação Android | `app/src/main/java/com/example/appfinanceiro/feature/relatorios/export/` | Somente transporte/armazenamento nativo; modelos, sheet, sucesso/erro e ViewModel são comuns |

O Android `:app` depende de `:sharedUi`. Não manter telas ou ViewModels no host.
`verifySharedPresentation`, incluído em `harnessVerify`, rejeita duplicação de
telas, apresentação nativa e imports de plataforma no código comum.

## Localizar uma funcionalidade

Na base `sharedUi/src/commonMain/kotlin/com/example/appfinanceiro/`:

| Fluxo | Fontes |
| --- | --- |
| Login/cadastro/recuperação | `feature/login/`, `feature/register/`, `core/data/AuthViewModel.kt`; sessão e bloqueio em `br/com/sobraai/ui/AppController.kt` |
| Visão mensal | `feature/home/HomeScreen.kt`, `HomeViewModel.kt`, resumo e diálogos em `feature/home/components/` |
| Despesas | `feature/despesas/DespesasScreen.kt`, `DespesasViewModel.kt`, filtros, detalhes, pagamento e adiantamentos |
| Criar/editar despesa | `feature/despesas/components/NovaDespesaScreen.kt`, `EditarDespesaScreen.kt`, seletores de data, rateio e categorias |
| Relatórios | `feature/relatorios/RelatoriosScreen.kt`, `RelatoriosViewModel.kt`; gráficos comuns em `designSystem` |
| Parcelados | `feature/relatorios/InstallmentCommitmentsScreen.kt`, `InstallmentCommitmentsViewModel.kt` |
| Exportar | `feature/relatorios/export/ReportExportSheet.kt`, `ReportExportViewModel.kt`, `ReportExportModels.kt`; `ReportFiles` isola o sistema de arquivos |
| Assistente | `feature/assistant/AssistantScreen.kt`, `AssistantViewModel.kt`; histórico completo, nova conversa, exclusão, cota e ditado |
| Perfil | `feature/perfil/PerfilScreen.kt`, `feature/perfil/components/EditarPerfilScreen.kt` |
| Rendas/categorias | `feature/perfil/components/ConfiguracoesRendaScreen.kt`, `CategoriasScreen.kt` |
| Mutação de dados | `core/data/FinanceActionsViewModel.kt`; repositórios/contratos em `shared` |

As pastas nem sempre coincidem com o package: `RegisterScreen.kt` usa o pacote
`feature.login`. Não renomear globalmente os packages, `applicationId` ou namespace.

## Regras que precisam sobreviver a alterações

- API externa: não há servidor neste repositório. Preservar endpoints, JSON,
  consultas, flags e os contratos de `FinanceDataSources.kt`.
- Financeiro: conferir parcelas, recorrência, períodos agendado/efetivo,
  adiantamentos, rateios, arredondamento e status. Não mudar saldo incidentalmente.
- Serviços nativos são injetados: sessão, biometria, galeria, voz, arquivos e
  permissões. Sem Android/Java em commonMain e sem UIKit nas telas.
- Cancelamento, expiração e bloqueio não podem publicar sucesso da sessão antiga.
- Senhas, tokens e dados pessoais não entram em logs, fixtures ou documentos.
- O backend ainda precisa definir proteção Apple para envio de códigos e versão
  de loja: [pendências externas](pendencias-backend-ios.md). Não enviar prova fictícia.

## Verificações

`harnessUnit` executa os testes JVM, incluindo os testes Android existentes contra
as classes agora comuns. `harnessVerify` acrescenta a guarda arquitetural, APK,
Lint e fontes/testes KLIB dos dois targets iOS. Veja [test-harness.md](test-harness.md).
Mac/Xcode/link e comparação visual Android/iPhone são verificações separadas.
O usuário prepara e testa os aparelhos; o agente não inicia dispositivos.
