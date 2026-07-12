# Regras de Front-end

Estas regras devem ser consideradas em toda mudança visual do aplicativo.

## Responsividade e acessibilidade

- Sempre considerar celulares com telas pequenas e estreitas, além de fontes maiores e teclado aberto.
- Em telas com conteúdo extenso, usar rolagem vertical e `imePadding()` para que os campos e botões não fiquem inacessíveis pelo teclado.
- Respeitar áreas seguras do sistema. Ações no topo, como voltar, devem usar `statusBarsPadding()`; ações no rodapé devem respeitar a barra de navegação.
- Manter alvos de toque de no mínimo 48 dp, principalmente ícones e ações secundárias.
- Evitar textos e controles com largura fixa. Preferir `fillMaxWidth()`, espaçamentos adaptáveis e textos que possam quebrar em mais de uma linha.

## Tema claro e escuro

- Toda nova interface deve ser conferida nos temas claro e escuro antes de ser considerada pronta.
- Usar `MaterialTheme.colorScheme` e os tokens de tema do projeto para fundo, superfícies, texto e erros; não fixar cores de fundo ou texto que prejudiquem um dos temas.
- Garantir contraste suficiente para textos, ícones, bordas, estados desabilitados e botões nos dois temas.
