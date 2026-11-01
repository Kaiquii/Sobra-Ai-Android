# Acervo do escopo

Autenticação: `sharedUi/.../feature/login/components/AuthComponents.kt` oferece
`AuthHeader`, `AuthTextField`, `AuthPasswordField`, `AuthPrimaryButton` e
`AuthErrorMessage`. Consumidores: `LoginScreen`, `RegisterScreen` e
`EsqueciSenhaScreen`. Campos e botões já usam raio 12 dp; o formulário de
recuperação tem e-mail, código, nova senha e confirmação, em página rolável.

Redesign aprovado de autenticação: `AuthPage.kt` centraliza marca, retorno,
áreas seguras e coluna rolável com footer opcional; consumidores LoginScreen,
RegisterScreen e EsqueciSenhaScreen. `AuthCodeField.kt` oferece um único valor
editável/colável com seis células visuais no cadastro. AuthHeader mantém
alinhamento central por default para GateScreen e aceita ícone nulo/alinhamento
à esquerda nas novas páginas. Todas as páginas de autenticação usam ícone nulo
no corpo, preservando a marca compacta do topo. Campos compartilham tema e ações IME opcionais.
AdvanceExpenseDialog e RemoveAdvanceDialog: sharedUi/.../feature/despesas/components/AdvanceExpenseDialogs.kt. ExpenseDetailsDialog e AdvanceDetailsBlock: sharedUi/.../core/designsystem/components/ExpenseDetailsDialog.kt. Consumidores DespesasScreen e HomeScreen; detalhes no Início não oferecem mutações de adiantamento.

Desde a alteração de 31/10/2026, ExpenseDetailsDialog aceita somente expense,
categoryName e onDismiss. AdvanceDetailsBlock é informativo; nenhum consumidor
pode injetar ação de adiantamento nos detalhes. Calendário do ExpenseCard e
AdvanceExpenseDialog preservam alterar data e remoção com confirmação.
O informativo de adiantamento apresenta somente a data em uma linha compacta;
fonte ampliada pode rolar horizontalmente para consultar o texto completo.

Extensão aceita: AdvanceExpenseDialog.onRemoveAdvanceClick opcional; ação visível somente isAdvanced+supportsAdvance, bloqueada por isUpdating; consumidor DespesasScreen. Não foi criado componente paralelo.
