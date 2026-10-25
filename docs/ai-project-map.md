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
| Despesas: apresentação | `designSystem/.../feature/despesas/components/ExpenseListControls.kt`, `core/designsystem/components/ExpenseCard.kt`, `ExpenseTypeStyle.kt` | Cabeçalho de mês/resumo/busca/filtros e cards usados em Início/Despesas; cor apenas no marcador do tipo da API; seletor de mês padrão e ações originais |
| Serviços da interface | `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/runtime/` | `UiServices`, contratos de arquivos, escopos de apresentação e formatação/data com adaptadores |
| Notificações de ações | `designSystem/.../core/designsystem/components/AppNotification.kt` | Card compacto reutilizável nos dois temas; eventos tipados de `UiServices`/`UiToast`, host acima da barra inferior medida, em `SharedApplication` |
| Seletores e dropdowns | `designSystem/.../core/designsystem/components/AppSelectField.kt` | Campo e modal de seleção únicos: formulários, rateios, filtros, exportação e projeção. Listas independentes do espaço da âncora, com seleção destacada e rolagem para listas longas |
| Janela dos seletores | `designSystem/.../core/designsystem/components/SelectorDialogWindow` (`commonMain`/`androidMain`/`iosMain`) | Somente propriedades nativas da janela. Android deixa Compose controlar os insets do teclado; layout e seleção continuam únicos em commonMain |
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
| Adiantar despesa | Apenas Única/Parcelada, pela regra comum `supportsExpenseAdvance` em `shared/.../core/network/ExpenseAdvanceEligibility.kt`; calendário azul no topo do `ExpenseCard`, diálogos e mutações protegidos. Fixa não oferece adiantamento; Paga continua independente |
| Relatórios | `feature/relatorios/RelatoriosScreen.kt`, `RelatoriosViewModel.kt`; gráficos comuns em `designSystem` |
| Parcelados | `feature/relatorios/InstallmentCommitmentsScreen.kt`, `InstallmentCommitmentsViewModel.kt` |
| Exportar | `feature/relatorios/export/ReportExportSheet.kt`, `ReportExportViewModel.kt`, `ReportExportModels.kt`; `ReportFiles` isola o sistema de arquivos |
| Assistente | `feature/assistant/AssistantScreen.kt`, `AssistantViewModel.kt`; componentes em `designSystem/.../feature/assistant/AssistantComponents.kt`; boas-vindas sem sugestões de mensagens, compositor integrado, histórico completo, nova conversa, exclusão, cota e ditado |
| Perfil | `feature/perfil/PerfilScreen.kt`, `feature/perfil/components/EditarPerfilScreen.kt` |
| Rendas/categorias | `feature/perfil/components/ConfiguracoesRendaScreen.kt`, `CategoriasScreen.kt` |
| VA e VR | Fontes canônicas em `shared/.../core/network/PaymentSources.kt`; renda mensal, despesas, filtros e resumo comuns. Contrato e validação em [beneficios-va-vr.md](beneficios-va-vr.md) |
| Mutação de dados | `core/data/FinanceActionsViewModel.kt`; repositórios/contratos em `shared` |

As pastas nem sempre coincidem com o package: `RegisterScreen.kt` usa o pacote
`feature.login`. Não renomear globalmente os packages, `applicationId` ou namespace.

O carregamento do Perfil é retido em `feature/perfil/ProfileLoadViewModel.kt`:
trocar de aba não cancela uma leitura em andamento nem repete automaticamente uma
leitura que falhou; o usuário pode tentar novamente pelo banner. A persistência
da identidade continua em `UiServices.persistProfileIdentity`, preservando biometria.

## Regras que precisam sobreviver a alterações

- API externa: não há servidor neste repositório. Preservar endpoints, JSON,
  consultas, flags e os contratos de `FinanceDataSources.kt`.
- Financeiro: conferir parcelas, recorrência, períodos agendado/efetivo,
  adiantamentos, rateios, arredondamento e status. Não mudar saldo incidentalmente.
- Serviços nativos são injetados: sessão, biometria, galeria, voz, arquivos e
  permissões. Sem Android/Java em commonMain e sem UIKit nas telas.
- Leituras HTTP GET repetem uma única vez após falha de conexão, com pausa curta;
  timeouts, respostas HTTP, JSON inválido e gravações não são repetidos pelo transporte.
  Consultas idênticas em andamento são reutilizadas; mês, filtro e revisão de dados
  salvos identificam uma consulta nova. Categorias não bloqueiam a lista de despesas.
- Relatórios, comparativo e compromissos também identificam consultas pelo período,
  filtros e revisão de dados. Ao repetir um carregamento inicial sem conteúdo,
  o erro anterior é limpo; quando existem dados antigos, o aviso permanece até sucesso.
- `loadNotices` é comum a `HomeLoadNotices`, `ExpensesLoadNotices` e
  `ReportsLoadNotices`: agrupa falhas de transporte simultâneas apenas quando
  têm o mesmo `NetworkFailureKind`. Erros HTTP e respostas
  inválidas continuam visíveis, sem novas repetições automáticas; timeout não é
  tratado como internet indisponível. Uma recuperação parcial preserva o erro restante.
- Cancelamento, expiração e bloqueio não podem publicar sucesso da sessão antiga.
- Biometria é exigida ao restaurar sessão em uma nova instância; alternar apps
  preserva sessão desbloqueada, tela e rascunhos. Hosts chamam `enterBackground()`,
  não `lock()`, ao minimizar. Autenticação inicial pendente continua protegida.
- Perfil oferece um interruptor em Segurança usando `SettingsToggleItem` do
  designSystem. A preferência vem de `SessionStore.biometricEnabledEmails` e a
  alteração passa por `AppController.setBiometricEnabled`: ativar exige biometria,
  desativar não altera o token nem consulta o backend. O estado só muda após salvar.
  A confirmação fica no status do próprio card, sem aviso flutuante de sucesso.
- Senhas, tokens e dados pessoais não entram em logs, fixtures ou documentos.
- O backend ainda precisa definir proteção Apple para envio de códigos e versão
  de loja: [pendências externas](pendencias-backend-ios.md). Não enviar prova fictícia.

## Verificações

`harnessUnit` executa os testes JVM, incluindo os testes Android existentes contra
as classes agora comuns. `harnessVerify` acrescenta a guarda arquitetural, APK,
Lint e fontes/testes KLIB dos dois targets iOS. Veja [test-harness.md](test-harness.md).
Mac/Xcode/link e comparação visual Android/iPhone são verificações separadas.
O usuário prepara e testa os aparelhos; o agente não inicia dispositivos.
