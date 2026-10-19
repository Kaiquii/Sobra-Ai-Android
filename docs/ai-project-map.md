# Mapa do projeto para IA

Todos os caminhos abaixo são relativos à raiz do repositório. Dentro dos fontes,
a base Android é `app/src/main/java/com/example/appfinanceiro/`. A biblioteca KMP
`:shared` usa `shared/src/commonMain/kotlin/com/example/appfinanceiro/` para as
regras comuns e mantém seus packages originais, sem alterações nos consumidores.
O projeto mantém **Android e iPhone**. Toda alteração deve avaliar os consumidores
das duas plataformas, conforme [rules.md](../rules.md); não restringir a validação
ao app Android ou apresentar compilação KLIB como execução iOS.

## Estrutura e pontos de entrada

`:sharedUi` contém login, cadastro/recuperação, Início, Rendas, Despesas,
Relatórios/Compromissos, Assistente e Perfil e seus apresentadores Compose comuns;
`iosApp/SobraAi.xcodeproj` hospeda o framework `SobraAiUI`. O Android `:app`
não depende desse módulo e mantém suas telas atuais. Configuração e limites:
[kmp-compose-ios.md](kmp-compose-ios.md).

Os fluxos editáveis comuns ficam em `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/`:
`FinanceWorkspace.kt` coordena navegação e invalidação; `FinanceEditors.kt` e
`FinancialForms.kt` tratam operações e validação; `ExpensesPresenter.kt` separa
períodos agendados/efetivos. `FinanceEditorScreens.kt`, `ExpensesScreen.kt` e
`IncomesScreen.kt` contêm as telas. `FinanceMutationDataSource.kt` em `:shared`
é o contrato de edição implementado pelo repositório comum, sem mudar
`FinanceDataSources.kt`. Regras e validação: [kmp-rendas-despesas.md](kmp-rendas-despesas.md).

`WorkspaceFeatures.kt` conecta `ReportsPresenter`, `AssistantPresenter` e
`ProfilePresenter` ao escopo da sessão. `FeatureState.kt` publica consultas com
filtros identificados e ações sem envio duplicado. `AccountAuthPresenter.kt`
coordena os formulários de cadastro/recuperação e a fronteira `AppleCodeDelivery`.
`IosNativeFeatures.kt` implementa fotos, compartilhamento e ditado com UIKit/Speech;
`NativeFeatures.kt` é seu contrato comum. `FinanceFiles.kt` em `:shared` atende
exportação e foto por Ktor, sem trocar a fronteira Retrofit do Android.
Detalhes: [funcionalidades finais iOS](kmp-funcionalidades-ios.md).
Envio de códigos e contrato de versão Apple: [pendências do backend](pendencias-backend-ios.md).

| Área | Caminho na base dos fontes | Responsabilidade |
| --- | --- | --- |
| Entrada | `MainActivity.kt` | Splash, inicialização de proteção, sessão, biometria e rotas Compose |
| Autenticação | `feature/login/`, `feature/register/` | Login, recuperação de senha e cadastro |
| Painel | `feature/home/` | Resumo, rendas, despesas e seleção de período |
| Despesas | `feature/despesas/` | Formulários, filtros, pagamento e adiantamento |
| Relatórios | `feature/relatorios/` | Gráficos, comparações e compromissos parcelados |
| Exportação | `feature/relatorios/export/` | Modelos, geração e armazenamento de relatórios |
| Perfil | `feature/perfil/` | Perfil, categorias, renda e ajuda |
| Assistente do app | `feature/assistant/` | Tela e estado da conversa com a API; separado das instruções de desenvolvimento para IA |
| Dados | `core/data/` | Repositórios, fontes de dados e ViewModels compartilhados |
| HTTP JSON | `shared/src/commonMain/kotlin/com/example/appfinanceiro/core/network/` (desde a raiz) | DTOs, serialização, contratos, Ktor e tratamento de erro |
| Arquivos HTTP Android | `core/network/FinanceApi.kt`, `core/network/auth/AuthApi.kt` | Retrofit somente para upload de foto e exportação; `RetrofitClient` fica em `AuthApi.kt` |
| Sessão comum | `shared/src/commonMain/kotlin/com/example/appfinanceiro/core/data/` (desde a raiz) | `SessionStore`, regras de sessão, persistência segura e contratos de repositório |
| Sessão por plataforma | `shared/src/androidMain/` e `shared/src/iosMain/` (desde a raiz) | `SessionManager` com DataStore; `IosSessionStore` com Keychain e preferências Apple |
| Proteções | `core/security/`, `core/biometric/`, `core/update/` | Play Integrity, biometria e exigência de atualização |
| Interface reutilizável Android | `core/designsystem/` | Tema, indicadores, banners, botões e diálogos do host Android |
| Interface KMP | `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/` (desde a raiz) | Telas, tema e apresentadores comuns consumidos pelo host iOS |
| Datas compartilhadas | `shared/src/commonMain/kotlin/com/example/appfinanceiro/core/date/` (desde a raiz) | Navegação de meses em Android/iOS |
| Validação compartilhada | `shared/src/commonMain/kotlin/com/example/appfinanceiro/feature/despesas/ExpenseFormValidator.kt` (desde a raiz) | Regras do formulário, sem Compose ou dependências Android |

`feature/register/RegisterScreen.kt` declara o pacote `feature.login`. Inspecione
declarações e imports antes de mover arquivos; não assuma correspondência perfeita
entre pacote e diretório.

As telas Configurações de Renda, Categorias, Ajuda e Suporte e Editar Perfil
compartilham cabeçalho, cards, títulos com ícones e botão principal em
`feature/perfil/components/ProfileSettingsComponents.kt`. Esses componentes são
internos à funcionalidade e usam o tema do app e `AppLoadingIndicator`.
Categorias e Renda usam a variante compacta de cards e ícones; Ajuda e Editar
Perfil preservam os espaçamentos padrão. O cadastro de categoria combina o campo
de nome e o botão de adicionar em uma linha.
Renda reutiliza `MonthSelector`; os tópicos de ajuda são expansíveis e preservam
o estado de expansão com `rememberSaveable`.
O seletor de mês de Renda fica diretamente na tela, como nos demais painéis.
Os campos de Renda, Editar Perfil e cadastro de categoria reutilizam
`feature/despesas/components/CustomInput.kt`, com fundo `expenseDetailBlock` dentro
dos cards. `SettingsTextField` agrupa rótulo externo e campo; o `CustomInput`
aceita prefixo opcional e estado habilitado, preservando os padrões dos demais
consumidores.

Em Despesas, `components/ExpenseFiltersDialog.kt` contém o botão e o modal de
categoria, origem e status. O modal mantém um rascunho até aplicar;
usa `ModalBottomSheet` e campos `ExposedDropdownMenuBox`, seguindo o padrão de
`feature/relatorios/export/ReportExportSheet.kt`.
`DespesasScreen.kt` guarda os valores aplicados e `ExpenseFilters.kt` combina os
critérios localmente com busca e tipo nos dados carregados do mês. Origens de
pagamentos divididos são resolvidas por `Expense.paymentSources()`.
O seletor de categoria oferece apenas categorias com despesas nos dados do mês,
incluindo adiantadas recebidas nesse período, sem restringir opções pela busca ou
pelos demais filtros. Uma categoria já aplicada é preservada ao mudar de mês;
o modal informa quando ela ficou sem despesas e permite trocar ou limpar.

## Fluxo de dados

`core/designsystem/components/FinancialPrivacy.kt` centraliza o estado de privacidade,
salvo com `rememberSaveable` acima do `NavHost` em `MainActivity.kt`. Início, Despesas,
Relatórios e Compromissos Parcelados compartilham o botão de olho e a mesma escolha.
`FinancialValue` substitui valores monetários por barras neutras, inclusive nos
detalhes e rateios de despesas; a edição de renda fica desabilitada neste modo.
`FinancialValue` mantém a medição e baseline do texto original e desenha apenas a
máscara quando oculto, removendo o texto das semânticas de acessibilidade.
`PrivateReportContent` substitui os relatórios ocultos por cards compactos com
skeletons, sem compor gráficos ou expor os valores e proporções à acessibilidade.
Essa compactação é específica dos relatórios; os demais valores mantêm seu espaço.
O card de saldo disponível não abre diálogo.

Em Home e Despesas, a tela consome o estado do ViewModel, que recebe uma interface
de fonte de dados. `FinanceRepository` implementa `HomeDataSource`,
`ExpensesDataSource`, `ReportsDataSource` e `AssistantDataSource`, definidos no
`core/data/FinanceDataSources.kt` de `:shared`. A implementação comum é
`SharedFinanceRepository`; o `FinanceRepository` Android mantém a mesma entrada
e acrescenta upload de foto nativo. `AndroidApiClient` injeta `BuildConfig.API_BASE_URL`
no transporte comum e preserva o tipo local `ApiRequestException` usado pelas telas.

O adaptador Android `core/network/FinanceJson.kt` mantém compatibilidade Gson para normalizar campos
textuais e rateios nulos/omitidos de despesas legadas, sem alterar valores, IDs ou
status. `Expense.displayDescription` fornece um aviso apenas visual para descrição
ausente; o aviso não é usado em cadastros/edições. Busca e origens de pagamento
também toleram campos nulos defensivamente. O caminho JSON ativo usa
`FinanceSerialization.kt` em `:shared`, com serializers próprios para esses campos
de apresentação e rateios. IDs, valores e status não são corrigidos para zero.

`AssistantChatResponse.tool_result` informa o estado da operação. Apenas
`tool_call=create_expense` com `status=created` incrementa a revisão de
`core/data/FinanceDataChanges.kt`. Home, Despesas, Relatórios (inclusive comparação)
e Compromissos Parcelados observam essa revisão para recarregar o período vigente.
A revisão permanece disponível enquanto a tela está fora da composição. Perguntas,
confirmações pendentes, cancelamentos e respostas antigas sem resultado estruturado
não sinalizam cadastro concluído. O chat mantém o `conversation_id` entre respostas.

`HomeViewModel` usa `StateFlow` para publicar seu estado. Antes de replicar esse
padrão em outra funcionalidade, leia seu ViewModel: a organização não é idêntica
em todas as telas.

`ApiTransport.kt` em `:shared` centraliza JSON HTTP: 401 autenticado
vira `SessionExpiredException`; 403 com mensagem de acesso revogado notifica
`SessionAccessEvents`; falhas de rede e timeout recebem mensagens próprias.
`MainActivity` observa revogação de acesso, limpa a sessão e navega ao login.
O executor Retrofit Android continua atendendo a fronteira de arquivos.
Leia também o tratamento no ViewModel afetado para não perder essas distinções.
Detalhes, integração iOS e limites: [kmp-api-session.md](kmp-api-session.md).

## Onde investigar cada mudança

`ExpenseFormValidator.kt` agora está na base comum de `:shared`; filtros,
adiantamentos e os demais arquivos desta tabela continuam na base Android.
DTOs de `FinanceModels.kt`/`auth/AuthModels.kt`, `FinanceDataSources.kt`,
`SessionExpiredException` e `SessionAccessEvents` também estão na base comum;
`SessionManager.kt` está em `shared/src/androidMain/.../core/data/`.

| Pedido | Comece por | Confira também |
| --- | --- | --- |
| Saldo ou renda incorreta | `feature/home/IncomeCalculations.kt`, `HomeViewModel.kt` | DTOs de resumo/renda e testes em `feature/home` |
| Despesa ou parcela incorreta | `feature/despesas/ExpenseFormValidator.kt`, `ExpenseFilters.kt`, `ExpenseAdvanceUtils.kt` | `DespesasViewModel.kt`, contratos de pagamento/rateio e testes correspondentes |
| Relatório incorreto | `feature/relatorios/RelatoriosViewModel.kt`, `InstallmentCommitmentsViewModel.kt` | `core/data/FinanceRepository.kt` e DTOs de relatório |
| Erro de login ou sessão | `core/data/AuthRepository.kt`, `AuthViewModel.kt` | `AuthApi.kt`, `ApiRequestExecutor.kt`, `SessionManager.kt`, `MainActivity.kt` |
| Erro somente em produção | `app/proguard-rules.pro`, `app/src/release/AndroidManifest.xml` | DTOs Gson/Retrofit, configuração de rede release e validação do AAB |
| Ajuste visual compartilhado | `core/designsystem/components/`, `core/designsystem/theme/` | Todos os consumidores e `docs/frontend-rules.md` |

Os caminhos abreviados nas colunas de investigação pertencem à mesma pasta do
primeiro arquivo da célula, salvo caminho explícito diferente.

## Fontes de configuração e testes

- `settings.gradle.kts`: módulos e repositórios de dependências.
- `build.gradle.kts`: grupos do harness. `harnessVerify` combina JVM, APK/Lint
  Android e compilação KMP; `harnessKmp` compila fontes/testes Apple dos dois
  módulos; `harnessIos`/`harnessIosTest` complementam a validação no Mac.
- `scripts/test-harness.ps1`: entrada Windows, modo padrão `verify`, checagem
  ADB só nos modos explícitos de dispositivo. Alcance em `docs/test-harness.md`.
- `shared/build.gradle.kts`: plugin Android KMP, target Android (minSdk 24,
  JVM 11), `iosArm64`/`iosSimulatorArm64` e framework estático `SobraAiShared`.
- `shared/src/commonTest/kotlin/`: testes `kotlin.test` de navegação mensal e
  formulário. O harness roda esses testes via `:shared:testAndroidHostTest`.
- `shared/README.md`: matriz de versões, comandos e integração Apple pendente.
- `app/build.gradle.kts`: identidade, SDK, variantes, dependências e URL da API.
- `gradle/libs.versions.toml`: catálogo de versões; ainda existem dependências
  declaradas diretamente no módulo.
- `gradle/gradle-daemon-jvm.properties`: JVM do daemon (atualmente 21). O nível
  Java de compilação no módulo é 11; são configurações distintas.
- `app/src/main/AndroidManifest.xml`: Activity de entrada e permissões.
- `app/src/release/res/xml/network_security_config.xml`: rede da variante release.
- `app/src/test/java/com/example/appfinanceiro/`: testes JVM de regras, erros e ViewModels.
- `app/src/test/java/com/example/appfinanceiro/MainDispatcherRule.kt`: dispatcher de testes.
- `app/src/androidTest/java/com/example/appfinanceiro/ExampleInstrumentedTest.kt`:
  verifica o packageName; sua aprovação não equivale a cobertura de fluxos de UI.

Exemplo real para novos testes de estado: `HomeViewModelTest.kt`, em
`app/src/test/java/com/example/appfinanceiro/feature/home/`, injeta uma fonte fake,
usa `runTest` com `MainDispatcherRule` e avança o scheduler antes das asserções.
