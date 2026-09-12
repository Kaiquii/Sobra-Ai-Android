# Fluxo de trabalho da IA

## Investigar e implementar

1. Confirme o pedido e o estado do Git. Localize o fluxo no
   [mapa do projeto](ai-project-map.md), leia os fontes e os testes pertinentes.
2. Identifique o contrato que deve permanecer estável: comportamento visual,
   dados da API, regras financeiras ou persistência. Para interface, aplique
   [as regras de front-end](frontend-rules.md).
3. Faça a alteração no componente responsável e nos consumidores necessários.
   Corrija a causa de regressões; não remova asserções ou suprima erros de Lint
   apenas para obter aprovação.
4. Para lógica nova ou corrigida, acrescente testes de comportamento relevantes.
   Use fakes e dados sintéticos; testes unitários não devem depender de login,
   rede real ou da disponibilidade do backend.
5. Execute a validação proporcional abaixo e revise o diff. Ao mudar caminhos,
   contratos ou comandos, atualize também a documentação afetada.

## Ambiente e comandos

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
| Lógica Kotlin/estado/API | `./gradlew.bat harnessVerify` e testes relevantes ao comportamento |
| Teste JVM específico durante investigação | `./gradlew.bat :app:testDebugUnitTest --tests "com.example.appfinanceiro.feature.home.HomeViewModelTest"` |
| Interface ou integração Android | Validação anterior, `./gradlew.bat :app:assembleDebug` e inspeção em dispositivo/emulador; aplicar regras visuais |
| Testes instrumentados existentes | `./scripts/test-harness.ps1 -Mode device` com dispositivo pronto |
| Todos os testes existentes e Lint debug | `./scripts/test-harness.ps1 -Mode all` |
| Preparação de release | `./gradlew.bat :app:lintRelease :app:testReleaseUnitTest :app:bundleRelease`, além de todos os testes automatizados aplicáveis |

Os exemplos com `./` também são aceitos no PowerShell. O script
[test-harness.ps1](../scripts/test-harness.ps1) é específico de Windows; veja
[sua documentação](test-harness.md) para os modos e os relatórios.

O build release usa R8 e redução de recursos. Contratos Gson/Retrofit são
acessados por reflexão: confira `app/proguard-rules.pro` ao alterá-los. Antes de
promover para produção, valide pela faixa de teste da Google Play o mesmo AAB,
conforme `frontend-rules.md`. Gerar o bundle não prova assinatura, instalação,
aprovação na loja ou funcionamento em produção.

## Como interpretar resultados

- Tarefa Gradle aprovada não prova que os fluxos visuais foram exercitados.
  O teste instrumentado de exemplo atual verifica somente o contexto do app.
- `UP-TO-DATE` indica reaproveitamento; não descreva isso como nova execução de
  cada teste. Force reexecução apenas quando for necessário para a investigação.
- Sem dispositivo, relate a validação Android pendente; não invente aprovação.
- Em falha de ambiente (SDK, Java, download ou URL), separe o problema de uma falha
  de teste. Não mude a configuração de produção para contornar o ambiente.
- Não repita números históricos de avisos/testes como se fossem resultados atuais.
  Consulte o relatório da execução correspondente.

## Critério de conclusão

Entregue a implementação solicitada, o diff revisado, os testes apropriados e a
documentação coerente. A resposta final deve dizer o que mudou, quais validações
foram feitas e quais não puderam ser feitas. Não faça commit, push ou publicação
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

Mantenha regras gerais no `AGENTS.md`, localização de código no mapa e comandos
neste fluxo. Atualize esses arquivos quando os fatos mudarem; não armazene neles
transcrições, tokens, relatórios temporários ou suposições como fatos.

Referência de carregamento consultada: [documentação oficial de AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md).
