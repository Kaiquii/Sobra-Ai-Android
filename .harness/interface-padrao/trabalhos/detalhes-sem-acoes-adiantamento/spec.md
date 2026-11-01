# Detalhes sem ações de adiantamento

Pedido direto e autorização de 31/10/2026: após concluir autenticação, retirar
os botões de adiantar/alterar/remover dos detalhes, mantendo o informativo.
Referência: captura fornecida pelo usuário, na qual Alterar data deve desaparecer.
Não redesenhar nem alterar outros blocos. O pedido delimita exatamente a mudança
visual autorizada; não há novo layout ou comportamento a escolher.

Reuso: ExpenseDetailsDialog/AdvanceDetailsBlock, nos consumidores DespesasScreen
e HomeScreen, comuns Android/iOS. Remover callbacks e ações dos detalhes, mantendo
data, status e aviso sobre Paga. Ações existentes no calendário do ExpenseCard
e AdvanceExpenseDialog/RemoveAdvanceDialog ficam inalteradas, incluindo bloqueios
de atualização, regras de Única/Parcelada e confirmação de remoção.

Sem novo componente, endpoint, cálculo ou alteração de versão. Revisar todos os
consumidores e rodar harnessVerify; conferência visual nos aparelhos pelo usuário.

## Ajuste solicitado após teste em 31/10/2026

O pedido posterior substitui a apresentação em duas linhas: manter somente
Adiantada em dd/MM/yyyy (ou Despesa adiantada sem data), em uma linha, com
preenchimento vertical reduzido. Em fonte ampliada/largura extrema, permitir
rolagem horizontal do texto para preservar a data completa, sem quebra.
A explicação de independência do status Paga permanece nos modais de operação.
Os detalhes usados em Início e Despesas compartilham esta mesma alteração.
