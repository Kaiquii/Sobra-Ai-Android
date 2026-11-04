# Padrões confirmados

04/11/2026, decisão mais recente VA/VR: Saldo disponível é a ÚLTIMA seção de
Configurações de Renda, depois de Renda Extra. Resumo curto e “?” junto ao
título abrem ajuda em modal, com exemplo e detalhes. Mock v2 aceito: “Aprovado,
só deixa o modal mais bonito”. Prevalece sobre a localização original abaixo
dos benefícios e antes de Renda Extra. Preservar cálculos e estados existentes.

04/11/2026: controles independentes de exclusão do restante de VA/VR ficam em
Configurações de Renda, por escolha explícita do usuário. Proposta visual v1
em trabalhos/preferencias-saldo-va-vr/spec.md, aceita pelo usuário em 04/11/2026,
reafirmando localização abaixo de VA/VR e texto explicativo.

Política Git explícita: não criar branches novas, inclusive temporárias/codex,
auxiliares ou por worktree. Conferir main antes do trabalho e de commit/push,
que continuam exigindo autorização. Preservar refs e alterações; divergência
de branch/HEAD destacado deve ser informada, sem checkout/reset/stash ou
exclusão automática para contorná-la. Não criar branches reais para testar.

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
