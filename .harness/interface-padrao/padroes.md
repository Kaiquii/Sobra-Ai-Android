# Padrões confirmados

31/10/2026: o usuário aprovou a proposta de autenticação do Stitch
(`projects/6150502095301066848`) e pediu visualizar Trocar senha antes da
implementação. A referência visual aprovada é login/cadastro: fundo escuro,
azul do app, formulário sem card externo e títulos de conteúdo à esquerda.
O pedido atual é somente mock; aprovação anterior não aprova uma nova tela.

Aceite posterior em 31/10/2026: “Gostei das melhorias, pode fazer”. Autoriza
implementar o conjunto de autenticação e a recuperação em duas etapas da v4.
Não acrescentar ajuda/expiração/garantias inventadas pelo gerador. Em Android e
iOS, usar shell comum e controles do acervo, mantendo envio após campos, marca
compacta e títulos à esquerda. Validação real nos aparelhos continua pelo usuário.
Pedido e aceite de 30/10/2026: manter modal de alteração de data, mover remoção dos detalhes para botão contornado largura total Remover adiantamento no modal. Proposta v2 explicitamente aceita; v3/v4 descartadas pelo usuário. Reusar AlertDialog/OutlinedButton/RemoveAdvanceDialog existentes. Preservar Paga, regras de elegibilidade, data editada ao cancelar confirmação e bloqueios durante atualização. Matriz: detalhe → Alterar data → modal de adiantamento → confirmação de remoção; cancelar confirmação retorna à edição, sucesso fecha ambos.

Atualização explícita de 31/10/2026, prevalece sobre a matriz anterior: nenhuma
ação de adiantamento permanece em Detalhes; manter apenas estado/data. Entrada
única pelas ações de calendário do card → modal de adiantamento → confirmação
de remoção. O usuário forneceu a captura e autorizou retirar somente os botões,
preservando o informativo e todo o restante do modal.
