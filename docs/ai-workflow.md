# Fluxo de trabalho da IA

O SobraAi é **Kotlin Multiplatform para Android e iPhone**. Toda mudança deve
considerar as duas plataformas, conforme [rules.md](../rules.md). O harness
padrão verifica o Android e a compilação KMP; build/execução Apple têm evidências
próprias e não podem ser presumidos a partir de um resultado JVM.

## Investigar e implementar

1. Confirme o pedido e o estado do Git. Localize o fluxo no
   [mapa do projeto](ai-project-map.md), leia os fontes e os testes pertinentes.
2. Identifique o contrato que deve permanecer estável: comportamento visual,
   dados da API, regras financeiras ou persistência. Para interface, aplique
   [as regras de front-end](frontend-rules.md).
   Identifique consumidores em `app`, `shared`, `designSystem`, `sharedUi` e `iosApp`; preserve
   o front-end Android e a compatibilidade dos contratos/adaptadores iOS.
3. Faça a alteração no componente responsável e nos consumidores necessários.
   Corrija a causa de regressões; não remova asserções ou suprima erros de Lint
   apenas para obter aprovação.
4. Para lógica nova ou corrigida, acrescente testes de comportamento relevantes.
   Use fakes e dados sintéticos; testes unitários não devem depender de login,
   rede real ou da disponibilidade do backend.
5. Execute a validação proporcional abaixo e revise o diff. Ao mudar caminhos,
   contratos ou comandos, atualize também a documentação afetada.

Para interface, inclua `sharedUi` e `designSystem` na investigação. Os dois hosts
devem abrir `SobraAiApp(services)`, com as mesmas telas, apresentação e navegação,
seguindo integralmente o Android de
referência. Preserve os adaptadores nativos e faça a comparação visual em
Android/iPhone com os mesmos dados, tema e configuração de fonte; o usuário
conduz essa comparação. Não aceite layouts iOS simplificados como conclusão.

## Ambiente e comandos

O usuário é responsável por preparar emuladores/simuladores e testar visualmente
o aplicativo nas duas plataformas. A IA não deve iniciar emuladores/simuladores,
instalar/abrir o app ou executar testes de dispositivo/Native em simulador sem
um pedido explícito para isso. As instruções de dispositivo
abaixo são referência para esse caso; não são autorização automática.
Na entrega usual, concluir `harnessVerify`: build Android, Lint, testes JVM e
fontes/testes KLIB de ambos os módulos KMP para iOS. Indique os cenários visuais
e as verificações Apple que o usuário deve conferir. Compilar testes sem
executá-los não exige iniciar um dispositivo.

Use a raiz do projeto como diretório de trabalho. No Windows, use PowerShell e
`gradlew.bat`; em Linux/macOS, use `./gradlew` com as mesmas tarefas. Prefira o
Wrapper ao Gradle instalado globalmente. Confira a JVM em
`gradle/gradle-daemon-jvm.properties` e o SDK em `app/build.gradle.kts`.

O SDK pode ser configurado por `sdk.dir` em `local.properties`. A URL da API deve
ser HTTPS e é resolvida nesta ordem: propriedade Gradle `-PAPI_BASE_URL`,
`local.properties`, variável de ambiente `API_BASE_URL`. Não imprimir o arquivo
local inteiro para diagnosticar uma configuração; confira apenas a presença dos
campos necessários. Não versionar configurações privadas.

Para testes JVM sem configuração de backend, pode-se fornecer um domínio
reservado que não representa um serviço real:

```powershell
.\gradlew.bat harnessUnit -PAPI_BASE_URL=https://example.invalid/
```

Esse valor serve para configuração de compilação de testes isolados. Não o use
para validar integrações ou gerar um artefato destinado ao usuário.

| Alteração/objetivo | Validação |
| --- | --- |
| Apenas documentação | Conferir caminhos, conteúdo, links locais e `git diff --check` |
| Lógica Kotlin/estado/API ou configuração | `./gradlew.bat harnessVerify` e testes relevantes; conferir impacto nos consumidores Android/iOS |
| Regras ou interface em `shared`/`sharedUi` | `./gradlew.bat harnessVerify`; link/build Apple no Mac e testes Native quando solicitados |
| Teste JVM específico durante investigação | `./gradlew.bat :app:testDebugUnitTest --tests "com.example.appfinanceiro.feature.home.HomeViewModelTest"` |
| Interface ou integração Android/iOS | `harnessVerify`; preservar o Android e validar o adaptador afetado; interface conferida pelo usuário nas duas plataformas |
| Frameworks Apple no Mac | `/bin/sh ./gradlew harnessIos`; também build do projeto Xcode, conforme README |
| Testes Native solicitados no simulador Apple | `/bin/sh ./gradlew harnessIosTest`, somente com Mac Apple Silicon/JDK ARM64 e simulador preparado |
| Testes instrumentados existentes | `./scripts/test-harness.ps1 -Mode device` com dispositivo pronto |
| Verificação KMP e instrumentados Android solicitados | `./scripts/test-harness.ps1 -Mode all`; testes Apple continuam separados |
| Preparação de release | `./gradlew.bat harnessUnit :app:lintRelease :app:bundleRelease`, além de todos os testes automatizados aplicáveis |

O script usa `verify` por padrão; `unit` é uma checagem parcial mais rápida.
Os exemplos com `./` também são aceitos no PowerShell. O script
[test-harness.ps1](../scripts/test-harness.ps1) é específico de Windows; veja
[sua documentação](test-harness.md) para os modos e os relatórios.

O build release usa R8 e redução de recursos. Contratos Gson/Retrofit são
acessados por reflexão: confira `app/proguard-rules.pro` ao alterá-los. Antes de
promover para produção, valide pela faixa de teste da Google Play o mesmo AAB,
conforme `frontend-rules.md`. Gerar o bundle não prova assinatura, instalação,
aprovação na loja ou funcionamento em produção.

Na configuração atual, `:app:tasks --all` registra `testDebugUnitTest`, mas não
`testReleaseUnitTest`. O harness executa testes JVM debug e testes comuns; não
apresentar isso como testes unitários de uma variante release. Para conferir
somente a compatibilidade do código com R8, existe `:app:minifyReleaseWithR8`,
sem empacotar nem publicar uma release.

## Como interpretar resultados

`harnessUnit` inclui `:app:testDebugUnitTest`, `:shared:testAndroidHostTest` e
`:sharedUi:testAndroidHostTest`. A interface comum e o host iOS têm verificações
adicionais em [kmp-compose-ios.md](kmp-compose-ios.md).
`harnessVerify` acrescenta a guarda `verifySharedPresentation`, APK debug, Lint
e `harnessKmp`. A guarda rejeita telas e ViewModels nativos paralelos; `harnessKmp` compila Android
e fontes/testes KLIB para `iosArm64` e `iosSimulatorArm64`, nos módulos `shared`
e `sharedUi`. A suíte comum executada nessa verificação roda na JVM; testes Apple
são apenas compilados. O Gradle avisa sobre testes Native desabilitados no Windows;
não suprimir o aviso nem contar tarefas ignoradas como aprovação iOS.
`harnessIos` faz link debug no Mac e `harnessIosTest` executa Native quando
solicitado; ambos recusam hosts sem suporte. Build Swift/Xcode e execução no
iPhone continuam provas distintas. Comandos e alcance em
[test-harness.md](test-harness.md) e no [README](../README.md#build-ios-no-mac).

- Tarefa Gradle aprovada não prova que os fluxos visuais foram exercitados.
  O teste instrumentado de exemplo atual verifica somente o contexto do app.
- `UP-TO-DATE` indica reaproveitamento; não descreva isso como nova execução de
  cada teste. Force reexecução apenas quando for necessário para a investigação.
- Sem dispositivo, relate a execução visual Android/iPhone pendente; sem Mac,
  registre também link/build Swift e testes Native pendentes. Não invente aprovação.
- Em falha de ambiente (SDK, Java, download ou URL), separe o problema de uma falha
  de teste. Não mude a configuração de produção para contornar o ambiente.
- Não repita números históricos de avisos/testes como se fossem resultados atuais.
  Consulte o relatório da execução correspondente.

## Critério de conclusão

Entregue a implementação solicitada preservando Android e iPhone, o diff revisado,
os testes apropriados e a documentação coerente. A resposta final deve separar
Android/JVM, compilação KMP, build/link Apple e execução real, informando as
validações que não puderam ser feitas. Não faça commit, push ou publicação
sem que isso faça parte do pedido.

## Uso e manutenção destas instruções

O ponto de entrada é [AGENTS.md](../AGENTS.md). O Codex descobre esse arquivo nas
instruções do projeto; em outras ferramentas, configure esse mesmo ponto de
entrada ou peça explicitamente sua leitura. Não presumir que qualquer IA lê
automaticamente os documentos referenciados.

Para conferir o carregamento em uma nova tarefa neste repositório, peça:
“Leia o AGENTS.md e os documentos indicados. Diga a diferença entre applicationId
e pacote Kotlin, onde fica o RetrofitClient e quais verificações este pedido
precisa, antes de editar.” Compare a resposta com o mapa e o fluxo acima.

Mantenha o ponto de entrada no `AGENTS.md`, política KMP em `rules.md`, localização
de código no mapa e comandos neste fluxo/harness. Atualize esses arquivos quando
os fatos mudarem; não armazene neles transcrições, tokens, relatórios temporários
ou suposições como fatos.

Referência de carregamento consultada: [documentação oficial de AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md).
