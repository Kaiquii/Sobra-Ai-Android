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

## Fluxo de dados

Em Home e Despesas, a tela consome o estado do ViewModel, que recebe uma interface
de fonte de dados. `FinanceRepository` implementa `HomeDataSource`,
`ExpensesDataSource`, `ReportsDataSource` e `AssistantDataSource`, definidos em
`core/data/FinanceDataSources.kt`, e chama `RetrofitClient.financeApi`.

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
