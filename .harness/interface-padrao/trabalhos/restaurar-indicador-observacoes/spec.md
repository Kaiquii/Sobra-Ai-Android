# Restaurar indicador de observações — proposta v1

Pedido de 01/11/2026: restaurar o pequeno ícone indicativo de observações nos
cards e encaminhar handoff ao projeto Sobra-Ai-Web para paridade mobile.
Fluxo completo por restaurar contrato de componente compartilhado.

Referência histórica: ExpenseCard.kt anterior ao commit 44f372a. O indicador
ExpenseNoteIndicatorIcon usava Description azul de 12 dp, fundo PrimaryBlue
a 10%, raio 6 dp e padding 4 dp; aparecia na linha do tipo/data somente quando
notes não era nulo, vazio ou composto por espaços. Não era um botão.

Reutilizar o ExpenseCard atual, Compact e Detailed, sem restaurar o ícone grande
de recibo, alterar ações, altura ou conteúdo das observações. Restaurar parâmetro
notes: String? = null no final da API para compatibilidade; encaminhar expense.notes
nos consumidores DespesasScreen e DespesasSection (Início). Mesmo Compose Android/iOS.
Descrição acessível: Tem observações. Texto completo permanece nos detalhes.

No Detailed, reservar espaço do indicador no final da linha de metadados, deixando
a data flexível. No Compact, manter o indicador após os metadados conforme o legado;
rever largura/fonte ampliada para não cortar o indicador ou deslocar ações.

Prévia ilustrativa externa: expense-notes-preview.png na área de visualizações
da sessão. Representa com/sem observações e adiantada; ações são esquemáticas,
e a implementação deve conservar os ícones atuais, não os símbolos da prévia.
Não é captura do aplicativo. Em 01/11/2026 o usuário respondeu “Aprovado”.

Critérios: indicador nos dois estilos somente com conteúdo; ausência com null,
vazio/espaços; data/Adiantada e ações preservadas; temas claro/escuro e fonte
ampliada. harnessVerify após implementação, conferência real pelo usuário.
Implementação: metadados de ambos os estilos rolam horizontalmente se faltar
espaço, com indicador reservado após o conteúdo e sem aumentar a altura. Isso
preserva leitura de data/marcadores com fonte ampliada; sem novo gesto de ação.

Handoff registrado em docs/handoff-web-indicador-observacoes.md e enviado ao chat
Ajustar avatar_url no front-end do Sobra-Ai-Web em 01/11/2026 após harnessVerify.
Implementação e validação web dependem do trabalho no chat de destino.
