# Mapa do projeto para IA

Todos os caminhos abaixo são relativos à raiz do repositório. Dentro dos fontes,
a base é `app/src/main/java/com/example/appfinanceiro/`.

## Estrutura e pontos de entrada

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
| HTTP | `core/network/FinanceApi.kt`, `core/network/auth/AuthApi.kt` | DTOs e serviços Retrofit; `RetrofitClient` fica em `AuthApi.kt` |
| Sessão | `core/network/session/SessionManager.kt` | Persistência via DataStore; seu pacote declarado é `core.data` |
| Proteções | `core/security/`, `core/biometric/`, `core/update/` | Play Integrity, biometria e exigência de atualização |
| Interface comum | `core/designsystem/` | Tema, indicadores, banners, botões e diálogos |
| Datas | `core/date/` | Navegação de meses |

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
`ExpensesDataSource`, `ReportsDataSource` e `AssistantDataSource`, definidos em
`core/data/FinanceDataSources.kt`, e chama `RetrofitClient.financeApi`.

`core/network/FinanceJson.kt` configura o Gson financeiro para normalizar campos
textuais e rateios nulos/omitidos de despesas legadas, sem alterar valores, IDs ou
status. `Expense.displayDescription` fornece um aviso apenas visual para descrição
ausente; o aviso não é usado em cadastros/edições. Busca e origens de pagamento
também toleram campos nulos defensivamente.

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

`core/data/ApiRequestExecutor.kt` centraliza `executeApiRequest`: 401 autenticado
vira `SessionExpiredException`; 403 com mensagem de acesso revogado notifica
`SessionAccessEvents`; falhas de rede e timeout recebem mensagens próprias.
`MainActivity` observa revogação de acesso, limpa a sessão e navega ao login.
Leia também o tratamento no ViewModel afetado para não perder essas distinções.

## Onde investigar cada mudança

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
