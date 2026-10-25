# Regras de Front-end

Estas regras devem ser consideradas em toda mudança visual do SobraAi,
projeto Kotlin Multiplatform para Android e iPhone. Aplicam-se às telas Android
compartilhadas em `sharedUi` e aos adaptadores dos dois hosts. Consulte também a
[política KMP](../rules.md) e o [harness](test-harness.md).

## Compatibilidade Android e iPhone

- **A interface deve ser igual nas duas plataformas, com fidelidade integral ao
  front-end Android de referência.** Funcionalidade equivalente com outro layout
  não atende. Todas as telas, navegação e apresentação ficam em
  `sharedUi/src/commonMain/`; os componentes ficam em `designSystem/src/commonMain/`.
  Ambos os hosts abrem `SobraAiApp(services)`. Não manter cópias independentes
  de telas ou ViewModels, nem uma segunda navegação para iOS.
  Diferenças nativas e critérios de conferência:
  [interface única](../rules.md#interface-única-android-e-ios-devem-ser-visualmente-iguais).

- Toda mudança visual ou de interação deve considerar as duas plataformas e os
  consumidores afetados. Preserve o front-end Android existente; uma adaptação
  iOS não autoriza alterações incidentais em suas telas, recursos ou navegação.
- Código comum deve usar APIs e recursos de Compose Multiplatform compatíveis
  com Android e os targets Apple. Isole serviços de plataforma em adaptadores;
  não importe APIs Android/JVM em `sharedUi/commonMain`.
- Confira teclado, áreas seguras de notch/indicador de início, navegação/voltar,
  rotação e fonte ampliada no iPhone, além das variações de tela Android.
  Acessibilidade inclui TalkBack e VoiceOver; ocultar valores também deve removê-los
  das semânticas nas duas plataformas.
- Biometria, fotos, arquivos, voz e permissões exigem tratamento nativo de
  cancelamento, indisponibilidade e segundo plano. Não presumir que o comportamento
  de um serviço Android existe de forma idêntica no iOS.
- Use `harnessVerify` para a verificação local Android/KMP. Sem execução em
  iPhone, registre a validação visual Apple pendente; não declare aprovação por
  compilar KLIB. O usuário conduz os testes manuais e prepara os dispositivos.

## Validação do build de produção

- No iOS, validar build/link Swift e Kotlin no Xcode, assinatura/provisionamento
  e o candidato no iPhone/TestFlight antes de distribuição. Sucesso do APK/AAB,
  dos testes JVM ou do KLIB não certifica o app Apple. O build comum de validação
  não autoriza gerar/publicar uma release em nenhuma loja.
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
- Os títulos das barras superiores ficam alinhados à esquerda (`TopAppBar`),
  inclusive Despesas Mensais, com os mesmos botões de voltar e ações de cada tela.
  Quando houver subtítulo, alinhar também à esquerda. Preservar os cabeçalhos de
  formulários de autenticação que fazem parte do conteúdo rolável.
- Em telas com conteúdo extenso, usar rolagem vertical e `imePadding()` para que os campos e botões não fiquem inacessíveis pelo teclado.
- O host Android usa `windowSoftInputMode="adjustResize"`: o teclado deve reduzir
  a área disponível, sem deslocar a janela inteira sobre a barra de status. No
  Assistente, o Scaffold trata o espaço do teclado uma vez e a lista reposiciona
  a última mensagem ao reduzir a altura, usando `requestScrollToItem` na medição,
  sem reiniciar animações de rolagem a cada quadro do teclado. As boas-vindas
  ficam compactas com pouca altura, mantendo o ícone menor e o conteúdo rolável;
  o aviso abaixo do campo
  aparece com o teclado fechado. Conferir abertura/fechamento do teclado,
  mensagem multilinha e rotação em Android e iPhone; não duplicar `imePadding()`
  no compositor e na lista.
- Respeitar áreas seguras do sistema. Ações no topo, como voltar, devem usar `statusBarsPadding()`; ações no rodapé devem respeitar a barra de navegação.
- No iOS, respeitar também notch e indicador de início. Conferir os insets já aplicados por Scaffold/host para não duplicar espaçamentos ao adaptar componentes comuns.
- Manter alvos de toque de no mínimo 48 dp, principalmente ícones e ações secundárias.
- Evitar textos e controles com largura fixa. Preferir `fillMaxWidth()`, espaçamentos adaptáveis e textos que possam quebrar em mais de uma linha.

## Tema claro e escuro

- Tipografia, pesos e métricas são definidos no tema comum com Roboto embutida
  em `designSystem/src/commonMain/composeResources/font/`. Não usar a fonte
  padrão diferente de cada sistema para textos do aplicativo.

- Toda nova interface deve ser conferida nos temas claro e escuro antes de ser considerada pronta.
- Usar `MaterialTheme.colorScheme` e os tokens de tema do projeto para fundo, superfícies, texto e erros; não fixar cores de fundo ou texto que prejudiquem um dos temas.
- Garantir contraste suficiente para textos, ícones, bordas, estados desabilitados e botões nos dois temas.
- Todos os modais e diálogos devem seguir o mesmo padrão visual do aplicativo: usar `MaterialTheme.colorScheme.background` como fundo, `onBackground` para título e conteúdo e os tokens do tema para ações. No modo escuro, não usar o fundo acinzentado padrão do Material sem uma decisão visual explícita.

## Apresentação de despesas

- A tela de despesas usa o seletor de mês padrão do aplicativo, resumo azul,
  busca e chips no cabeçalho comum `ExpenseListControls`; ele rola com a lista para continuar acessível com
  teclado ou fonte ampliada. O total mantém o período efetivo e os filtros atuais.
- `ExpenseCard` é compartilhado entre Início e Despesas. Não reservar coluna para
  o antigo ícone grande de recibo, nem exibir a carta de observações; o conteúdo das
  observações continua nos detalhes.
- Somente o marcador do tipo usa azul para Única, roxo para Parcelada e verde-água
  para Fixa, com tons adequados ao tema. Não colorir nem adicionar bordas ao card,
  preservar a hierarquia de nome, categoria, tipo, valor e origem. `ExpenseTypeStyle`
  recebe o tipo original da API, separado do rótulo de parcela (`Parc. 3/10`). Não usar origem, categoria
  ou status de pagamento para escolher essa cor. Manter o texto do tipo: a cor
  sozinha não identifica a despesa para todos os usuários.
- Nos cards detalhados de Despesas, preservar o tamanho compacto e as ações no
  canto superior direito, na mesma linha: pagar, adiantar, visualizar,
  editar e excluir. O usuário pediu explicitamente essa disposição como na web;
  não criar uma linha de ações no rodapé nem aumentar a altura dos cards para
  acrescentar um botão. A referência web orienta somente a posição; manter os
  ícones e cores do Android: calendário azul com seta para adiantar e calendário
  com marca de confirmação nas despesas já adiantadas, para alterar a data.
  Remover o adiantamento continua nos detalhes. Não alterar Paga como consequência
  de adiantar ou remover o adiantamento.

## Textos e idioma

- Dropdowns usam `AppSelectField` e `AppSelectDialog` do designSystem. Usar o
  mesmo campo e lista de seleção nos formulários, rateios, filtros e relatórios;
  não reintroduzir menus ancorados que comprimem listas curtas. O modal adapta
  a largura e a altura à área segura, permite rolagem quando necessário e mantém
  opções com 48 dp mínimos, sem cortar nomes. Fechar a seleção não aplica uma
  opção; filtros continuam sujeitos à confirmação no botão Aplicar filtros.
  A padronização da lista não autoriza redesenhar os controles ao redor: preservar
  `CustomInput`, `FormLabel`, margens, tipografia e os botões existentes (incluindo
  o “+” verde dentro do campo Categoria). Usar o slot `anchor` quando necessário
  para conservar a apresentação original. A busca discreta pertence ao modal,
  ignora caixa/acentos e filtra os rótulos sem mudar IDs nem aplicar uma seleção.
  Manter o modal intermediário (largura máxima de 440 dp e cerca de sete opções por
  vez), com opções em `bodyLarge`, sem ocupar quase toda a tela nem reduzir demais
  a lista. A altura depende da lista original e do espaço seguro, nunca da quantidade
  de resultados da busca: digitar não deve encolher/recentralizar a janela.
  Reservar o espaço do botão de limpar mesmo com a busca vazia e reiniciar a
  lista no primeiro resultado ao alterar a consulta, sem rolagem animada.
  O seletor usa um host de janela de tamanho integral, com painel visual limitado
  e fechamento ao tocar fora. No Android, `selectorDialogProperties` define
  `decorFitsSystemWindows=false`: os insets animados de Compose são o único dono
  do deslocamento pelo teclado, evitando pan/redimensionamento concorrente da
  janela flutuante. O adaptador Apple mantém as propriedades comuns. Não adicionar
  animação de tamanho ou um segundo `imePadding` por cima da área segura.
- Manter nomes como “Vale-alimentação (VA)” e “Vale-refeição (VR)”, com acentos e
  siglas maiúsculas. Nos editores de renda, separar a ação do nome completo com
  hierarquia tipográfica para evitar um título excessivamente grande.

- Todo texto exibido ao usuário deve estar em português brasileiro, com ortografia, acentuação, concordância e pontuação revisadas.
- Manter os arquivos de código e recursos em UTF-8; limitações técnicas não justificam remover acentos dos textos da interface.
- Antes de concluir uma mudança, revisar títulos, botões, campos, mensagens de erro, estados vazios, notificações e descrições de acessibilidade.
- Não alterar nomes de campos, valores ou mensagens que façam parte do contrato com a API. Quando necessário, adaptar apenas o texto apresentado na interface.

## Entrada com biometria

- Com sessão salva e biometria ativada, exibir a mesma tela de login enquanto
  o sistema solicita automaticamente digital, Face ID ou Touch ID. Não criar tela
  intermediária de desbloqueio nem exigir toque para iniciar a autenticação.
- Após cancelamento, manter o login com tentativa manual de biometria ou senha;
  nunca repetir o prompt em ciclo nem expor conteúdo financeiro antes da confirmação.

## Estados de carregamento

- Mensagens transitórias de ações usam `AppNotification`, do designSystem, em
  um único `SnackbarHost` compacto acima da barra inferior medida, dentro da área
  segura e acima do teclado. Usar superfície do tema e ícone por tipo; confirmações
  fecham ao toque e avisos/erros têm botão de fechar. `UiToast.success/error/warning` e
  `UiServices.notify` encaminham mensagens tipadas; não inferir sucesso pelo texto.
  Uma mensagem nova substitui a anterior, sem acumular confirmações antigas.
  O espaço inferior deve ser a união da área segura/teclado com a altura medida
  da barra, usando o maior valor. Não alternar o espaço da barra por um booleano
  de teclado visível nem somar novamente seus insets: isso causa salto ao salvar
  e fechar o teclado. A regra vale para todos os formulários e diálogos.
  Falhas de carregamento continuam persistentes na própria tela, com retry.
- Os avisos de Início, Despesas e Relatórios usam `loadNotices`: agrupar falhas de
  transporte iguais e repetir somente as consultas afetadas. Manter erros HTTP
  distintos separados. Se um painel já exibir o aviso central, seus componentes
  internos não devem duplicá-lo. O retry no banner usa ícone de atualizar com
  descrição acessível para liberar largura para a mensagem; não cortar o texto.

- Falha de consulta não significa lista vazia: mostrar contagem indisponível (—)
  enquanto os dados não foram carregados, em vez de afirmar zero despesas.
- Categorias carregam independentemente das despesas; falha nos nomes/filtros
  não deve bloquear a lista e os valores. Exibir o aviso específico e permitir
  repetir somente a consulta de categorias. Se ambas falharem com o mesmo tipo
  de falha de transporte, usar um aviso único com retry das duas consultas;
  não agrupar erros HTTP distintos nem considerar timeout como ausência de internet.

- Usar o componente `AppLoadingIndicator` para manter tamanho, cor e espessura consistentes nos indicadores de carregamento.
- Exibir o carregamento central apenas na primeira abertura, quando ainda não existe conteúdo disponível.
- Em atualizações de mês, data ou filtros, manter o conteúdo atual na tela e mostrar somente o indicador compacto no topo. A interface não deve piscar nem ser substituída por uma tela vazia.
- Se uma atualização falhar e a tela continuar mostrando dados anteriores, avisar claramente que eles podem estar desatualizados. Usar `AppDataErrorBanner` com superfície do tema, borda e ícone de erro e ação “Tentar novamente”. Falha de comunicação deve mencionar conexão com o servidor, sem afirmar que a internet do usuário caiu. Preservar mensagens de erro da API e distinguir demora na resposta.
- Ao tocar em “Tentar novamente” com dados antigos visíveis, manter o aviso, trocar a ação por “Tentando...” com indicador de progresso e impedir toques repetidos até a requisição terminar. Remover esse aviso somente após uma resposta bem-sucedida.
- Sem conteúdo carregado, uma nova tentativa deve mostrar carregamento e limpar o erro da tentativa anterior. Se a nova tentativa também falhar, mostrar o erro terminal; não esconder falhas reais nem repetir consultas em ciclo. Reabrir uma aba não deve cancelar uma leitura idêntica ainda em andamento.
- Em telas de painel, como Relatórios, uma falha inicial não deve desmontar a estrutura nem deixar uma grande área vazia. Manter ações e cards em seus estados vazios ou zerados, acompanhados do aviso de erro para deixar claro que os valores não foram carregados.
- Nunca apresentar dados antigos como se pertencessem ao novo mês, data ou filtro sem informar o usuário.
