# Regras KMP — Android e iPhone

O SobraAi é um projeto **Kotlin Multiplatform para Android e iOS**. Toda mudança
deve preservar o funcionamento nas duas plataformas. Uma tarefa só parecer
correta no Android não basta para declarar a compatibilidade KMP aprovada.

O ponto de entrada para agentes continua sendo [AGENTS.md](AGENTS.md). Este
arquivo concentra a política KMP; os comandos estão no
[harness](docs/test-harness.md) e no [fluxo de validação](docs/ai-workflow.md).

## Implementação nas duas plataformas

### Interface única: Android e iOS devem ser visualmente iguais

- **O front-end Android existente é a referência oficial. O iOS deve reproduzir
  100% dessa interface**, incluindo estrutura das telas, ordem das informações,
  cores, tipografia, ícones, cards, espaçamentos, botões, estados, modais e navegação.
  Paridade apenas de funcionalidades ou dados **não atende** ao projeto.
- Não criar uma versão simplificada, aproximada, genérica ou com outro desenho
  para o iPhone. Não substituir os componentes do app pelos padrões Material
  apenas porque compilam. Não modificar o Android para imitar uma tela iOS divergente.
- Use Compose Multiplatform e **uma única implementação de TODAS as telas,
  navegação, estado e apresentação**, em `sharedUi/src/commonMain/`, com os
  componentes reutilizáveis de `designSystem/src/commonMain/`.
  **Android e iOS chamam exatamente `SobraAiApp(services)`**, definido em
  `sharedUi/src/commonMain/kotlin/br/com/sobraai/ui/SharedApplication.kt`.
  `:app` depende de `:sharedUi`. Compartilhar apenas DTOs, regras, tema ou cards
  enquanto cada plataforma mantém suas próprias telas **é proibido**.
- Não criar `HomeScreen`, formulários, ViewModels, navegação ou apresentação
  alternativos em `app`, `androidMain`, `iosMain`, Swift ou UIKit. Não manter
  telas iOS reduzidas, paralelas, provisórias ou de fallback. Alterar a tela
  comum para que a mesma mudança chegue aos dois aplicativos.
- Os hosts somente criam/injetam os serviços nativos e encaminham o ciclo de
  vida. Adaptadores nativos não podem decidir layout, filtros, regras financeiras,
  conteúdo das telas ou destinos de navegação. `UiServices` é a fronteira; a
  apresentação não pode importar Android, Java, Retrofit ou UIKit.
- Toda mudança de interface deve chegar às duas plataformas na mesma tarefa.
  Uma correção de divergência do iOS deve preservar o desenho Android;
  a extração de código comum deve conservar esse desenho.
- A tipografia usa a família Roboto embutida em `designSystem/commonMain/composeResources`.
  Não deixar a fonte padrão de cada sistema substituir a referência Android.
  Manter os mesmos tamanhos, pesos, espaçamentos e alturas de linha no tema comum.
- Diferenças são permitidas somente quando exigidas pela plataforma ou solicitadas
  pelo usuário: áreas seguras, teclado, permissões, seletores/compartilhamento do
  sistema e biometria real. No iPhone com Face ID, apresentar Face ID e seu ícone;
  com Touch ID, apresentar Touch ID e impressão digital. Detectar o sensor com
  LocalAuthentication; não oferecer os dois nomes juntos como se fossem a mesma opção.
- O ícone instalado no iOS deve usar a mesma identidade visual do Android,
  incluído em um AppIcon válido no catálogo de assets e ligado ao target Xcode.
- **Não declarar igualdade visual comprovada por build ou testes JVM/KLIB.**
  A comparação real é feita pelo usuário em Android e iPhone, nas mesmas telas,
  temas e condições, incluindo fonte ampliada. Sem essa conferência, declarar
  explicitamente a validação visual pendente e corrigir qualquer divergência relatada.

### Código, contratos e adaptadores

- Antes de alterar código, identifique os consumidores em `app`, `shared`, `designSystem`,
  `sharedUi` e `iosApp`, inclusive quando o pedido mencionar apenas uma plataforma.
- Coloque regras de domínio, contratos HTTP, repositórios, sessão e apresentação
  reutilizável em `commonMain` quando forem compatíveis. Não duplique uma regra
  no Android e no iOS para contornar uma dependência de plataforma.
- Não importe Android, JVM, Retrofit, OkHttp ou APIs Java em `commonMain`.
  Dependências novas precisam suportar os targets Android, `iosArm64` e
  `iosSimulatorArm64`; conferir versões, recursos e APIs antes de adotar.
- Isole armazenamento, sensores, arquivos, voz, permissões e ciclo de vida em
  contratos injetáveis com adaptadores nativos. Ao alterar um contrato ou
  `expect`/`actual`, atualize todas as implementações e os fakes afetados.
- Preserve endpoints, nomes JSON, regras financeiras, cancelamento, erros e
  identidade de mês/filtro nas duas plataformas. Acrescente testes comuns para
  comportamento compartilhado; mantenha testes específicos quando necessários.
- A migração mantém o desenho completo do Android em código comum. Não redesenhar
  o Android para facilitar a migração. Cadastro, recuperação, detalhes, filtros,
  modais, editores, exportação, histórico, configurações e telas secundárias
  obedecem à mesma regra das telas principais; nenhuma pode ficar fora da unificação.
- Preserve HTTPS, sessão segura, privacidade e biometria. Play Integrity continua
  no Android; proteção Apple exige contrato próprio. Não simule provas, não
  desative proteção e não trate Face ID como substituto de integridade do app.
- Consulte [as regras de interface](docs/frontend-rules.md) para teclado, áreas
  seguras, temas, fontes, acessibilidade e interações nas duas plataformas.

## Validação obrigatória e evidências

- Para alterações de código ou configuração, use `harnessVerify`, incluindo a
  guarda arquitetural `verifySharedPresentation`: testes JVM dos
  três módulos, APK Android debug, Lint e compilação dos fontes/testes KLIB dos
  dois targets iOS, nos módulos `shared` e `sharedUi`, além dos fontes visuais
  de `designSystem` para Android e ambos os targets Apple.
- `harnessUnit` é uma verificação rápida e parcial. Seu sucesso não substitui
  `harnessVerify` nem o build/execução Apple. `harnessKmp` também é parcial: compila
  os targets, mas não executa testes JVM/Native, Swift ou interface.
- Em Mac/Xcode, complemente com `harnessIos` (link dos frameworks debug) e build
  do projeto Xcode. Execute `harnessIosTest` somente quando solicitado, com
  simulador ARM64 preparado; ele pode iniciar o simulador.
- Não inicie emuladores/simuladores, instale/abra apps ou execute testes de
  dispositivo por iniciativa própria. O usuário conduz a validação visual/manual.
- Sem Mac/Xcode/iPhone, conclua as verificações disponíveis e registre o que
  permanece pendente. Não conte tarefa desabilitada, ignorada ou `--dry-run`
  como teste aprovado. KLIB compilado no Windows não comprova link nem execução iOS.
- Mudanças somente documentais exigem revisão de conteúdo, links e
  `git diff --check`; não exigem recompilar o aplicativo.
- Na entrega, separe evidências Android/JVM, compilação KMP, build/link Apple e
  execução real. Não prometa funcionamento completo nas duas plataformas sem
  as respectivas verificações. Não faça release/publicação como efeito da tarefa.

Dependências externas conhecidas ficam em
[pendencias-backend-ios.md](docs/pendencias-backend-ios.md); seu registro não
autoriza enfraquecer as proteções ou declarar um fluxo pendente como funcional.
