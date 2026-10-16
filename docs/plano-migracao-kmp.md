# Plano de migração do SobraAi para Kotlin Multiplatform

Data: 16/10/2026. Status: proposta para implementação futura.

Este documento planeja a evolução do aplicativo Android para Android e iOS,
com Kotlin Multiplatform (KMP) e Compose Multiplatform. Sua criação não altera
código, dependências, configuração, versão ou assinatura do aplicativo.

## 1. Objetivo e recomendação

Compartilhar regras financeiras, acesso à API, estado de apresentação e o máximo
viável da interface, preservando o funcionamento do Android. Manter integrações
com o sistema operacional em implementações específicas de cada plataforma.

A recomendação é migrar progressivamente no repositório atual, começando por uma
versão pequena que faça login e carregue o Início no iPhone. Não converter todas
as telas antes de comprovar compilação, sessão, API e interface no ambiente Apple.

KMP compartilha código; Compose Multiplatform é a escolha proposta para compartilhar
também as telas. Uma interface iOS inteiramente em SwiftUI continua sendo uma
alternativa, mas exigiria manter duas implementações de UI. Para o SobraAi, o uso
atual de Compose favorece experimentar primeiro a interface compartilhada.

A documentação oficial descreve migração gradual de aplicativos Compose existentes,
com isolamento de dependências Android/JVM. Essa é a referência técnica da estratégia,
não uma garantia de reaproveitamento automático de cada API usada no SobraAi.
[Guia de migração](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html).

## 2. Diagnóstico do projeto atual

Evidências consultadas no repositório:

- Um módulo Android `:app`, configurado em [app/build.gradle.kts](../app/build.gradle.kts).
- `namespace` e `applicationId` iguais a `br.com.sobraai.app`; pacote Kotlin
  predominante `com.example.appfinanceiro`. Não unificar nomes incidentalmente.
- Kotlin, Jetpack Compose, coroutines, StateFlow e Navigation Compose.
- [MainActivity.kt](../app/src/main/java/com/example/appfinanceiro/MainActivity.kt)
  concentra entrada, rotas, sessão, biometria e integração com proteções.
- [FinanceDataSources.kt](../app/src/main/java/com/example/appfinanceiro/core/data/FinanceDataSources.kt)
  fornece interfaces para Home, Despesas, Relatórios e Assistente.
- `FinanceRepository` implementa essas interfaces, mas também contém operações
  adicionais e tipos de transporte específicos, como `MultipartBody.Part`.
- `RetrofitClient` está em `core/network/auth/AuthApi.kt`; a comunicação financeira
  usa Retrofit, OkHttp e Gson, incluindo normalização de JSON legado.
- `SessionManager` usa Preferences DataStore e recebe `Context` Android.
- `AuthRepository` chama Play Integrity diretamente para solicitar código de
  cadastro e recuperar senha; login e envio do cadastro têm chamadas separadas.
- `AppVersionRepository.getAndroidVersion()` envia `platform = "android"`.
- Exportação baixa PDF, XLSX ou CSV do servidor. O cliente persiste o arquivo;
  não é necessário recriar um gerador desses formatos no iOS.
- Há testes JVM com fontes fake e testes instrumentados Android. A cobertura
  existente é uma base de regressão, não comprovação de funcionamento no iOS.

As versões atuais incluem AGP 9.2.0 e Kotlin 2.2.10 no catálogo. A matriz de
compatibilidade com Gradle, Compose Multiplatform, plugin Android KMP e Xcode deve
ser verificada na execução. Este plano não fixa versões novas nem recomenda
atualizar todas as dependências ao mesmo tempo.

## 3. Matriz de reaproveitamento

Os níveis abaixo são avaliações qualitativas da leitura dos fontes. Não representam
percentuais medidos nem código já compilado para iOS. Caminhos abreviados partem
de `app/src/main/java/com/example/appfinanceiro/`.

| Área e fontes | O que reaproveitar | O que precisa mudar | Esforço relativo |
| --- | --- | --- | --- |
| `feature/home/IncomeCalculations.kt` e `feature/despesas/ExpenseFormValidator.kt` | Regras e comportamento das funções | Mover para código comum e portar os testes | Baixo |
| `feature/despesas/ExpenseFilters.kt` e `ExpenseAdvanceUtils.kt` | Critérios de filtro e interpretação de adiantamentos | Remover dependências JVM quando presentes; manter semântica das datas | Baixo a médio |
| `core/date/MonthNavigation.kt` | Modelo mês/ano e regra de deslocamento | Substituir `Math.floorMod`/`floorDiv` por solução comum equivalente, inclusive para valores negativos | Baixo |
| `core/network/FinanceApi.kt` | Nomes JSON, endpoints, parâmetros, tipos e valores do domínio | Separar DTOs das declarações Retrofit; adaptar serialização e transporte | Alto |
| `core/network/FinanceJson.kt` | Regras de tolerância a campos textuais/rateios nulos ou omitidos | Reimplementar normalização na nova serialização, sem zerar valores financeiros inválidos | Médio |
| `core/data/FinanceDataSources.kt` | Interfaces, injeção por construtor e fakes | Mover junto dos modelos usados nas assinaturas; preservar contratos | Baixo a médio |
| `FinanceRepository`, `AuthRepository`, `ApiRequestExecutor` | Operações e comportamento de erros | Desacoplar Retrofit, exceções Java, multipart Android/JVM e proteção de dispositivo | Alto |
| ViewModels de Home, Despesas, Relatórios e Assistente | Estados, eventos e regras de carregamento | Usar dependências multiplataforma compatíveis; retirar `android.util.Log` e outros acoplamentos | Médio a alto |
| `core/data/FinanceDataChanges.kt` | Revisão de mudanças confirmadas pelo assistente | Compartilhar e preservar atualização de telas fora da composição | Baixo |
| `core/designsystem/` | Tokens, cores, componentes, privacidade e linguagem visual | Auditar APIs Android, fontes, recursos e acessibilidade no iOS | Médio |
| Telas em `feature/` | Layouts Compose e comportamento dos formulários | Extrair contexto, navegação e serviços nativos; validar teclado, gestos e áreas seguras | Alto no conjunto |
| `MainActivity.kt` | Rotas e regras de acesso | Manter entrada Android e extrair raiz Compose compartilhada; criar entrada iOS | Médio a alto |
| `core/network/session/SessionManager.kt` | Modelo de sessão, preferências e biometria por usuário | Criar contrato de sessão e implementações por plataforma | Médio |
| `core/biometric/BiometricAuth.kt` | Fluxo de habilitação, sucesso e falha | Implementar autenticação iOS com LocalAuthentication e política explícita de fallback | Médio |
| `core/security/PlayIntegrityProtection.kt` | Implementação Android e intenção de proteção | Criar abstração e negociar mecanismo iOS com o backend | Alto; dependência externa |
| `core/update/` e `AppVersionRepository` | Regra de atualização opcional/obrigatória | Versão e link de loja por plataforma; confirmar contrato iOS no servidor | Médio; dependência externa |
| Perfil, fotos e Coil 2 | Fluxo de seleção, upload, remoção e atualização de avatar | Avaliar Coil 3; seleção, permissões, orientação EXIF e dados da imagem por plataforma | Médio a alto |
| `feature/relatorios/export/` | Tipos de relatório, parâmetros, nomes e estados | Substituir `ResponseBody`, `Uri`, `MediaStore`, streams Java e `AndroidViewModel` nas fronteiras compartilhadas | Alto |
| `app/src/test/` | Casos, dados sintéticos, fakes e asserções de comportamento | Migrar casos comuns para `commonTest`; manter testes Android específicos | Médio |
| Manifest, recursos Android, splash e regras R8 | Configuração e proteção do aplicativo Android | Permanecer no host Android; criar equivalentes Apple onde necessário | Médio |

## 4. Arquitetura proposta

Começar com dois módulos novos no máximo, além do host Android existente:

```text
:app                         Host Android atual
:shared                      Biblioteca KMP de domínio, dados e apresentação
  src/commonMain/            Modelos, regras, contratos, repositórios e estados
  src/commonTest/            Testes multiplataforma
  src/androidMain/           Adaptadores Android usados pela biblioteca
  src/iosMain/               Adaptadores Apple usados pela biblioteca
:sharedUi                    Biblioteca Compose Multiplatform
  src/commonMain/            Tema, componentes, telas e navegação compartilhados
  src/androidMain/           Integrações de UI Android que forem necessárias
  src/iosMain/               Ponte da UI para o host iOS
iosApp/                      Projeto Xcode e entrada Swift/SwiftUI/UIKit
```

Os nomes são propostos. `iosApp/` é o host Xcode, não necessariamente um módulo
Gradle. `:app` continua como aplicação Android; bibliotecas KMP não assumem esse
papel. Conferir o plugin de biblioteca Android KMP adequado ao AGP na fase inicial.
[Estrutura oficial](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html).

Direção das dependências: hosts → `sharedUi` → `shared`. O módulo `shared` não
depende de Activities, `Context`, `Uri`, Compose de interface ou recursos `R`.
Inicialização e injeção das implementações ficam na composição dos hosts.

Preservar injeção manual por construtor. Interfaces são suficientes para a maior
parte dos serviços; usar `expect/actual` apenas quando trouxer vantagem concreta.
Não introduzir framework de DI nem criar um módulo por tela sem necessidade.

Contratos propostos para fronteiras nativas: armazenamento de sessão, biometria,
proteção de dispositivo, metadados do aplicativo, abertura de links, seleção de
imagem e persistência/compartilhamento de arquivos. Nomes e assinaturas serão
definidos na implementação, preservando as interfaces públicas já existentes.

## 5. Decisões técnicas a comprovar

### Rede e JSON

Proposta: Ktor Client e kotlinx.serialization no código comum, com engine OkHttp
no Android e Darwin no iOS. Ktor oferece engines por plataforma; a combinação
exata deve ser validada no protótipo.
[Engines do Ktor](https://ktor.io/docs/client-engines.html).

Pode-se manter Retrofit no Android durante a transição, com adaptadores para os
mesmos contratos. Evitar manter duas implementações completas indefinidamente.

Antes de trocar o transporte, registrar testes para autorização, nomes JSON,
campos ausentes/nulos, corpos de erro, timeout, cancelamento e multipart. A
normalização atual do Gson não pode desaparecer silenciosamente com serializers
mais estritos. DTOs de fronteira podem precisar aceitar nulos antes do mapeamento
para modelos de domínio. Não transformar qualquer erro em resposta vazia.

Preservar `period_mode = "effective"`, pagamento, adiantamento e rateio. Manter
401 autenticado como sessão expirada e 403 de acesso revogado como evento de
limpeza de sessão. Conferir separadamente a exportação, que hoje tem tratamento
próprio de erro. Não adicionar retry automático a criações financeiras sem
garantia de idempotência do backend.

### Estado, datas e dinheiro

Coroutines e StateFlow são candidatos ao compartilhamento. Lifecycle, ViewModel
e Navigation têm suporte multiplataforma, mas exigem coordenadas/versões
compatíveis e validação do ciclo de vida no iOS.
[Empacotamento das bibliotecas](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-jetpack-libraries.html).

Separar estado de apresentação de dependências Android antes de mover ViewModels.
Revisar cancelamento e respostas concorrentes: uma resposta antiga não pode
substituir os dados do período atual. A revisão é requisito de migração, não uma
declaração de que todos os ViewModels atuais já tratam esses casos.

Substituir `Calendar`, `SimpleDateFormat`, `Locale` e outras APIs JVM onde usadas
por soluções comuns ou adaptadores. Avaliar kotlinx-datetime e APIs Kotlin
compatíveis com a matriz escolhida. Testar virada de ano, fuso e datas sem horário.
Não reinterpretar uma data financeira local como instante UTC automaticamente.

Manter `Double`, unidade monetária e arredondamento atuais durante a migração.
Uma futura mudança de representação monetária precisa de tarefa própria. Isolar
formatação de BRL/pt-BR e demonstrar equivalência entre plataformas.

### Sessão e serviços Apple

Preservar o DataStore Android e suas chaves na primeira etapa para não perder
sessões/preferências de usuários existentes. Definir armazenamento iOS de token
em Keychain e preferências não sensíveis em mecanismo apropriado à plataforma.
Não assumir que todo dado deve ir para o mesmo armazenamento.

Biometria iOS exige integração própria, configuração de uso do Face ID quando
aplicável e testes de recusa, indisponibilidade, cancelamento e retorno ao app.
O token persistido não deve liberar telas protegidas antes do desbloqueio exigido.

## 6. Dependências externas e ambiente

| Dependência | Evidência atual | Ação antes de considerar iOS completo |
| --- | --- | --- |
| Mac e Xcode | Ambiente de trabalho informado é Windows | Obter acesso local/remoto, definir versões compatíveis e compilar uma aplicação iOS mínima |
| API de autenticação protegida | Requests contêm `protection_provider` e `play_integrity_token` | Confirmar contrato e validação de proteção iOS com o responsável pelo backend |
| Atualização | Consulta explícita de `platform = "android"` e abertura da Play Store | Confirmar suporte a iOS, versão mínima e URL da App Store |
| Assinatura/distribuição | Configuração Apple não estabelecida nesta investigação | Definir bundle ID, equipe, certificados, perfis e acesso necessário para TestFlight/App Store |
| Validação física | Nenhum fluxo foi executado no iPhone nesta tarefa | Disponibilizar iPhone e responsável pela validação manual |

macOS e Xcode são necessários para compilar e executar a aplicação iOS. O Windows
permite parte da preparação compartilhada, mas não substitui essa validação.
[Ambiente KMP](https://kotlinlang.org/docs/multiplatform/recommended-ides.html).

App Attest é um candidato para a proteção Apple, sujeito à avaliação do backend
e da política de suporte. Não é substituição direta do token Play Integrity;
precisa de protocolo e validação no servidor. Não desabilitar a proteção Android
nem inventar um token iOS para liberar cadastro ou recuperação.
[App Attest](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity).

O backend é externo a este repositório. O suporte real a iOS ainda não foi
confirmado. Login existente pode permitir o primeiro protótipo se o servidor o
aceitar, mas isso não comprova cadastro e recuperação de senha no iPhone.

## 7. Etapas de execução

Cada etapa deve produzir uma alteração pequena e revisável. Não avançar uma
dependência crítica com base apenas em compilação Android.

| Etapa | Trabalho | Entrega e critério de saída |
| --- | --- | --- |
| 0 — Baseline e viabilidade | Registrar validação Android; inventariar APIs/imports e recursos; confirmar Mac, backend e matriz de versões | Android de referência validado, bloqueios externos identificados e ambiente Apple mínimo compilando |
| 1 — Fundação KMP | Criar `shared`, hosts e integração; manter `:app`; mover funções de menor acoplamento e testes | Regras compartilhadas compilam para Android/iOS e passam nos testes comuns das duas plataformas |
| 2 — API e sessão mínimas | Implementar transporte comum de login/resumo/rendas/despesas e fronteira de sessão | Contratos equivalentes demonstrados por testes; sessão e carregamento comprovados no iOS |
| 3 — Primeira interface iPhone | Criar `sharedUi`; migrar tema, componentes mínimos, login e Início | Fluxo login → resumo do mês → logout funciona no iPhone; Android continua aprovado |
| 4 — Núcleo financeiro | Migrar Despesas, formulários, pagamento, adiantamento, filtros e configuração de renda/categorias | Operações e consultas equivalentes, incluindo parcelas, recorrência e rateio |
| 5 — Relatórios e assistente | Migrar gráficos, comparações, compromissos parcelados e conversa | Revisão de dados após criação confirmada e relatórios por período demonstrados nas duas plataformas |
| 6 — Integrações e paridade | Finalizar biometria, fotos, arquivos, cadastro, recuperação, atualização e telas restantes de perfil/ajuda | Todas as funcionalidades do escopo Android têm equivalente iOS validado; dependências do backend resolvidas |
| 7 — Estabilização e distribuição | Revisão de acessibilidade, ciclo de vida, segurança, desempenho, assinatura e pacotes de distribuição | Candidato testado em iPhone e Android; distribuição de teste validada; publicação somente em tarefa autorizada |

Investigar proteção Apple já na etapa 0 e construir sua integração durante as
etapas anteriores conforme o backend ficar disponível. Ela não deve aparecer
como surpresa apenas na etapa 6. O mesmo vale para seleção de fotos e arquivos.

### Primeira entrega executável: escopo fechado

Incluir login, persistência/limpeza de sessão, Início com resumo, rendas e despesas
do período, navegação de mês, carregamento, erro e privacidade dos valores.
Usar conta de teste autorizada para a integração; testes automatizados usam fakes
e dados sintéticos.

Essa entrega prova o caminho iOS → código compartilhado → API → estado → tela.
Não deve ser anunciada como migração concluída: cadastro, recuperação, biometria,
edição financeira, exportação e demais telas ainda precisam das etapas seguintes.

### Exportação e fotos: critérios próprios

Na exportação, compartilhar parâmetros e download sem obrigar carregar arquivos
inteiros em memória. Persistir com MediaStore/seleção de destino no Android e
sandbox/exportação/compartilhamento apropriados no iOS. Usar uma referência de
arquivo independente de `android.net.Uri` nos estados comuns. Testar fechamento
de recursos, cancelamento, remoção de arquivo parcial e falta de espaço.

Nas fotos, preservar tipo MIME, multipart, limites do servidor, orientação e
atualização do cache do avatar. Testar formatos aceitos pelo seletor iOS, incluindo
a necessidade de conversão de HEIC, sem presumir que o backend já os aceita.

## 8. Validação e preservação do Android

Antes da implementação, executar na raiz com o Wrapper:

```powershell
.\gradlew.bat harnessUnit
.\gradlew.bat harnessVerify
.\gradlew.bat :app:assembleDebug
```

Repetir os checks pertinentes a cada mudança. `harnessVerify` já inclui os testes
JVM e Lint debug. Ao criar módulos KMP, atualizar o harness para incluir os testes
comuns executados nos targets aplicáveis; hoje ele verifica apenas `:app`.
Comandos de testes iOS serão definidos após configurar targets e ambiente, sem
inventar tarefas Gradle como se já existissem.

Reutilizar os casos de renda, filtros, adiantamentos, navegação de mês, JSON legado,
erros de API, privacidade, exportação e assistente. Portar testes comuns para
`kotlin.test` e coroutines-test, preservando testes Android específicos. Acrescentar
casos de concorrência/cancelamento onde necessário para o comportamento migrado.

Checklist funcional para a migração:

- Mesmos dados sintéticos produzem os mesmos resultados financeiros e filtros.
- Campos JSON nulos/omitidos mantêm a tolerância visual existente sem alterar IDs,
  valores, status ou persistência indevidamente.
- Pagamento, rateio, recorrência, parcelas e adiantamentos preservam seus contratos.
- Troca rápida de mês, falha de conexão e retry não rotulam dados antigos como atuais.
- Sessão expirada, acesso revogado, logout e falha de biometria bloqueiam o acesso esperado.
- Apenas `create_expense` com resultado `created` sinaliza nova despesa no assistente.
- Temas claro/escuro, fontes ampliadas, teclado, áreas seguras e leitores de tela
  funcionam; valores ocultos não ficam disponíveis à acessibilidade.
- Retorno do segundo plano, recriação de tela e navegação mantêm estado adequado.
- Exportação e fotos funcionam no dispositivo, incluindo erros e cancelamentos.

O usuário inicia emuladores e faz a validação visual/manual Android. Não iniciar,
instalar ou abrir o app nem executar testes instrumentados sem solicitação
explícita. Alinhar também a execução manual iOS quando a implementação começar.

Antes de uma futura release Android, validar Lint/testes release e AAB com R8
habilitado, seguindo [ai-workflow.md](ai-workflow.md) e
[frontend-rules.md](frontend-rules.md). Não remover regras de reflexão enquanto
Retrofit/Gson ou seus consumidores ainda dependerem delas.

No iOS, build e testes no simulador não substituem validação de biometria,
proteção de dispositivo e distribuição em iPhone real. Validar o candidato via
TestFlight antes de considerar publicação. Nenhuma publicação está autorizada
pela criação deste plano.

## 9. Riscos, controle de escopo e reversão

| Risco | Controle proposto |
| --- | --- |
| Troca de serializer altera contratos ou dados legados | Fixtures sintéticas e testes de equivalência antes de trocar consumidores |
| Migração quebra Android em uso | Mover por funcionalidade, preservar host e persistência, validar cada etapa |
| Incompatibilidade entre plugins/bibliotecas | Provar matriz em aplicação mínima e registrar versões antes da migração ampla |
| Backend não suporta proteção/versão iOS | Confirmar cedo; registrar bloqueio externo sem contornar segurança |
| Código Compose usa serviços Android indiretamente | Auditar imports, recursos e assinaturas ao mover cada componente |
| Respostas concorrentes exibem período errado | Testes com respostas fora de ordem e identidade do período carregado |
| Interface compila, mas fica inadequada ao iPhone | Validar teclado, gestos, acessibilidade, escalas e áreas seguras em dispositivo |
| Crescimento do projeto por duas implementações | Adaptadores temporários com critério de remoção e contratos comuns testados |

Planejar commits/PRs pequenos para a implementação futura, conforme autorização
da tarefa. Cada incremento deve permitir reversão sem exigir migração destrutiva
de dados. Evitar mudança do esquema de sessão e das regras financeiras no mesmo
incremento que introduz KMP. Não usar uma flag de execução para manter duas
aplicações completas sem necessidade.

Não entram automaticamente no escopo: redesenho completo, renomeação global de
pacotes, backend novo, alteração de unidade monetária, suporte web/desktop,
framework de DI, assinatura Android nova ou publicação nas lojas.

## 10. Esforço e decisões pendentes

Trata-se de migração de porte médio a grande. O reaproveitamento mais direto está
nas regras e componentes desacoplados; o maior esforço está em transporte,
integrações nativas, telas com serviços Android e validação das duas plataformas.

Como ordem de grandeza, planejar semanas a alguns meses, não uma conversão em um
dia. Não há prazo fechado neste documento: faltam a prova de compilação Apple,
o inventário completo por tela e a confirmação do backend. Estimar em dias de
trabalho por etapa após a primeira entrega; separar esforço de implementação de
espera por infraestrutura, backend e distribuição. Não atribuir percentual de
reuso antes de medir os arquivos migrados e seus testes.

Decisões necessárias para iniciar e fechar o planejamento operacional:

1. Qual Mac/Xcode e iPhone estarão disponíveis para desenvolvimento e validação?
2. A primeira entrega pode ser login + Início, mantendo paridade completa como objetivo final?
3. Quem confirmará e implementará suporte iOS à proteção de autenticação e à consulta de versão?
4. Qual versão mínima de iOS e quais tamanhos/dispositivos serão suportados?
5. Quem controla a conta Apple, assinatura, identificador e distribuição de teste?
6. Há ambiente/contas de teste autorizados para validar os fluxos integrados?

Essas perguntas são dependências para implementação futura; não impedem a entrega
deste plano nem significam que o suporte iOS já foi confirmado.

## 11. Critério de migração concluída

- Android preserva funcionalidades e passa nas verificações apropriadas.
- iOS executa os fluxos do escopo completo em iPhone real, com evidências registradas.
- Código comum contém apenas dependências compatíveis com os targets definidos.
- Proteção, sessão, atualização, fotos e exportação têm implementações válidas nas duas plataformas.
- Regras financeiras e contratos HTTP têm testes de equivalência relevantes.
- Harness, mapa do projeto e documentação refletem os módulos e comandos finais.
- Candidatos de distribuição são validados nos canais de teste das plataformas.

Compilar a primeira tela, gerar um framework iOS ou produzir um pacote não satisfaz
sozinho esses critérios.

## 12. Estado desta entrega

Foi feita análise estática dos fontes, configuração e testes selecionados, com
consulta às referências oficiais vinculadas acima. Este arquivo é um plano;
nenhum módulo KMP, projeto Xcode ou adaptação do backend foi implementado.

A revisão desta entrega deve conferir conteúdo, caminhos locais e
`git diff --check`. Build, Lint, testes JVM, simulador e dispositivo não são
necessários para criar o documento e não foram executados nesta tarefa.
