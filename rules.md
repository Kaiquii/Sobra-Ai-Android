# Regras KMP — Android e iPhone

O SobraAi é um projeto **Kotlin Multiplatform para Android e iOS**. Toda mudança
deve preservar o funcionamento nas duas plataformas. Uma tarefa só parecer
correta no Android não basta para declarar a compatibilidade KMP aprovada.

O ponto de entrada para agentes continua sendo [AGENTS.md](AGENTS.md). Este
arquivo concentra a política KMP; os comandos estão no
[harness](docs/test-harness.md) e no [fluxo de validação](docs/ai-workflow.md).

## Implementação nas duas plataformas

- Antes de alterar código, identifique os consumidores em `app`, `shared`,
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
- O `app` Android mantém seu front-end existente. A adoção de KMP não autoriza
  redesenhar, substituir ou migrar suas telas incidentalmente. O host iOS usa
  `sharedUi`; paridade funcional não exige copiar integrações Android para Apple.
- Preserve HTTPS, sessão segura, privacidade e biometria. Play Integrity continua
  no Android; proteção Apple exige contrato próprio. Não simule provas, não
  desative proteção e não trate Face ID como substituto de integridade do app.
- Consulte [as regras de interface](docs/frontend-rules.md) para teclado, áreas
  seguras, temas, fontes, acessibilidade e interações nas duas plataformas.

## Validação obrigatória e evidências

- Para alterações de código ou configuração, use `harnessVerify`: testes JVM dos
  três módulos, APK Android debug, Lint e compilação dos fontes/testes KLIB dos
  dois targets iOS, nos módulos `shared` e `sharedUi`.
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
