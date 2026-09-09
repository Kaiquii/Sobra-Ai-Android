# Regras de Front-end

Estas regras devem ser consideradas em toda mudança visual do aplicativo.

## Validação do build de produção

- Nunca considerar o funcionamento do build `debug` como validação suficiente do aplicativo publicado. O build `release` usa R8 e precisa ser validado separadamente.
- Manter redução de código, otimização, ofuscação e redução de recursos habilitadas no `release`. Contratos acessados por reflexão devem possuir regras de preservação específicas e revisadas.
- Antes de gerar uma versão para a Play Console, executar o lint de release, todos os testes automatizados e a geração do AAB sem erros.
- Antes de promover uma versão para produção, instalar pela faixa de teste da Google Play o mesmo AAB que será promovido e validar os fluxos principais, integrações externas, persistência local, arquivos e comportamento offline.
- Não enviar uma versão diretamente para produção com base apenas em testes pelo Android Studio ou em APK instalado manualmente.

## Responsividade e acessibilidade

- Toda implementação visual deve considerar celulares com diferentes tamanhos, proporções e densidades de tela. O layout precisa se adaptar corretamente a telas compactas, convencionais e grandes, incluindo aparelhos dobráveis e uso em modo de tela dividida.
- Não assumir uma largura fixa disponível. Componentes, indicadores, diálogos, cards e áreas de navegação devem respeitar os limites do contêiner, manter margens mínimas e encolher, reorganizar ou permitir rolagem quando o espaço for reduzido.
- Antes de concluir uma mudança visual, conferir o comportamento em diferentes larguras de viewport e com os ajustes de tamanho da interface e da fonte aumentados. Nenhum elemento pode encostar indevidamente nas bordas, sobrepor outro conteúdo, ficar cortado ou perder sua área mínima de toque.
- Sempre considerar celulares com telas pequenas e estreitas, além de fontes maiores e teclado aberto.
- Em telas com conteúdo extenso, usar rolagem vertical e `imePadding()` para que os campos e botões não fiquem inacessíveis pelo teclado.
- Respeitar áreas seguras do sistema. Ações no topo, como voltar, devem usar `statusBarsPadding()`; ações no rodapé devem respeitar a barra de navegação.
- Manter alvos de toque de no mínimo 48 dp, principalmente ícones e ações secundárias.
- Evitar textos e controles com largura fixa. Preferir `fillMaxWidth()`, espaçamentos adaptáveis e textos que possam quebrar em mais de uma linha.

## Tema claro e escuro

- Toda nova interface deve ser conferida nos temas claro e escuro antes de ser considerada pronta.
- Usar `MaterialTheme.colorScheme` e os tokens de tema do projeto para fundo, superfícies, texto e erros; não fixar cores de fundo ou texto que prejudiquem um dos temas.
- Garantir contraste suficiente para textos, ícones, bordas, estados desabilitados e botões nos dois temas.
- Todos os modais e diálogos devem seguir o mesmo padrão visual do aplicativo: usar `MaterialTheme.colorScheme.background` como fundo, `onBackground` para título e conteúdo e os tokens do tema para ações. No modo escuro, não usar o fundo acinzentado padrão do Material sem uma decisão visual explícita.

## Textos e idioma

- Todo texto exibido ao usuário deve estar em português brasileiro, com ortografia, acentuação, concordância e pontuação revisadas.
- Manter os arquivos de código e recursos em UTF-8; limitações técnicas não justificam remover acentos dos textos da interface.
- Antes de concluir uma mudança, revisar títulos, botões, campos, mensagens de erro, estados vazios, notificações e descrições de acessibilidade.
- Não alterar nomes de campos, valores ou mensagens que façam parte do contrato com a API. Quando necessário, adaptar apenas o texto apresentado na interface.

## Estados de carregamento

- Usar o componente `AppLoadingIndicator` para manter tamanho, cor e espessura consistentes nos indicadores de carregamento.
- Exibir o carregamento central apenas na primeira abertura, quando ainda não existe conteúdo disponível.
- Em atualizações de mês, data ou filtros, manter o conteúdo atual na tela e mostrar somente o indicador compacto no topo. A interface não deve piscar nem ser substituída por uma tela vazia.
- Se uma atualização falhar e a tela continuar mostrando dados anteriores, avisar claramente que eles podem estar desatualizados. Para falhas de conexão, usar o componente compacto `AppDataErrorBanner` com a mensagem “Sem conexão. Exibindo os últimos dados carregados.” e oferecer a ação “Tentar novamente”.
- Ao tocar em “Tentar novamente”, manter o aviso visível, trocar a ação por “Tentando...” com indicador de progresso e impedir toques repetidos até a requisição terminar. Remover o aviso somente após uma resposta bem-sucedida.
- Em telas de painel, como Relatórios, uma falha inicial não deve desmontar a estrutura nem deixar uma grande área vazia. Manter ações e cards em seus estados vazios ou zerados, acompanhados do aviso de erro para deixar claro que os valores não foram carregados.
- Nunca apresentar dados antigos como se pertencessem ao novo mês, data ou filtro sem informar o usuário.
