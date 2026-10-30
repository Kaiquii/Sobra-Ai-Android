# Acervo do escopo
AdvanceExpenseDialog e RemoveAdvanceDialog: sharedUi/.../feature/despesas/components/AdvanceExpenseDialogs.kt. ExpenseDetailsDialog e AdvanceDetailsBlock: sharedUi/.../core/designsystem/components/ExpenseDetailsDialog.kt. Consumidores DespesasScreen e HomeScreen; detalhes no Início não oferecem mutações de adiantamento.

Extensão aceita: AdvanceExpenseDialog.onRemoveAdvanceClick opcional; ação visível somente isAdvanced+supportsAdvance, bloqueada por isUpdating; consumidor DespesasScreen. Não foi criado componente paralelo.
