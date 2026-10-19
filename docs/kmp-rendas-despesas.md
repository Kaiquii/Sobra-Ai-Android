# Edição de rendas e despesas no iOS

## Escopo implementado

O módulo `:sharedUi` agora oferece navegação entre Início, Despesas e Rendas,
além do formulário de adicionar despesa. `iosApp` continua apenas hospedando
Compose; formulários, estado, validação e chamadas à API são Kotlin comum.
O Android `:app` continua consumindo somente `:shared`, com seu front-end intacto.

| Fluxo | Comportamento e contrato preservado |
| --- | --- |
| Criar renda | Mês/ano explícitos, `type=Fixa`; salário usa `Salario`; `repeat_future` aparece somente para Renda Extra |
| Editar renda | Valor e `update_future`, habilitado por padrão; confirmação antes de aplicar aos próximos meses |
| Excluir renda | Confirmação; `delete_future=true` somente quando selecionado, caso contrário parâmetro omitido |
| Criar despesa | Categoria, descrição, data, origem simples ou rateada, tipo Única/Fixa/Parcelada, parcelas e observações |
| Editar despesa | Carrega o registro pelo ID; mantém tipo, número de parcelas, status Paga e adiantamento; confirma atualização futura de fixas/parceladas |
| Criar categoria | A partir do formulário; seleciona a categoria retornada pela API e mantém o rascunho |
| Excluir despesa | Fixa mantém o contrato sem `delete_future` e avisa que removerá ocorrências futuras; Parcelada permite selecionar `delete_future=true`; Única exclui a ocorrência |
| Pagamento | Confirma marcar como Paga ou Pendente; altera somente `is_paid` pelo endpoint específico |
| Adiantamento | Data obrigatoriamente anterior à prevista, podendo ser futura; usa `yyyy-MM-dd`; alteração/remoção não muda o status de pagamento |
| Lista e totais | Consulta mês agendado e período efetivo separadamente; mostra despesas recebidas por adiantamento e calcula total pelo período efetivo, sem duplicar IDs |
| Filtros | Busca por descrição/valor, tipo, categoria, origem (inclui rateios) e status; aplicação/cancelamento do rascunho de filtros |

O formulário continua usando `Double` e não arredonda o valor enviado à API.
Rendas e parcelas do rateio precisam ser positivas; despesas simples preservam
o zero aceito pelo formulário Android. A edição também preserva valores negativos
legados sem trocar seu sinal ou reescrever a natureza da despesa. Números malformados, agrupamento ambíguo,
NaN e infinito são rejeitados em vez de convertidos silenciosamente para zero.
A soma dos rateios conserva a tolerância de `0.009` existente no Android.
Observações têm limite de 500 caracteres. Datas inválidas não viram a data de hoje
sem escolha do usuário. A edição não reescreve o tipo nem o número de parcelas.

Os seletores de data usam dias UTC; o payload da data da despesa mantém meia-noite
com o fuso local, como no Android. O adiantamento envia somente a data civil.
Não foram adicionados endpoints, valores de domínio ou cálculos financeiros no cliente.

## Estado e proteção contra regressões

- `FinanceMutationDataSource` em `:shared` expõe as operações editáveis sem
  dependência de Activity, ViewModel ou engine HTTP. `SharedFinanceRepository`
  implementa o contrato; `FinanceDataSources.kt` e a injeção de fakes existentes
  permanecem disponíveis.
- `FinanceWorkspace` possui o escopo da sessão financeira e coordena navegação
  e recarga de Início/Despesas após gravações bem-sucedidas.
- `IncomeEditor`, `ExpenseEditor` e `MutationRunner` bloqueiam envios repetidos,
  mantêm dados/erros em falhas e propagam cancelamento. Uma resposta atrasada
  após logout/bloqueio não publica sucesso nem recarrega a sessão anterior.
- `ExpensesPresenter` publica um snapshot por mês, com categorias, despesas
  agendadas e efetivas. Respostas de períodos anteriores não substituem o atual.
  Dados antigos permanecem identificados e o aviso continua durante a tentativa.
- O resumo recebido precisa corresponder ao período solicitado; rendas de outros
  meses não entram nos valores apresentados. A escolha de categoria permanece
  ao mudar de mês, mesmo sem despesas, com opção de limpar/trocar.
- Privacidade é comum às telas. Valores e rateios ocultos saem da leitura de
  acessibilidade; edição de renda/despesa existente fica desabilitada nesse modo.
  Bloqueio biométrico e logout descartam os editores e cancelam suas operações.
- Textos, confirmações, campos com rolagem/teclado, áreas seguras e alvos de toque
  foram preparados para as duas plataformas. A aprovação visual exige execução.

## Validação e build Apple

Use os mesmos comandos de [integração Compose](kmp-compose-ios.md):

```powershell
.\gradlew.bat harnessVerify :app:assembleDebug
.\gradlew.bat :sharedUi:compileKotlinIosArm64 :sharedUi:compileKotlinIosSimulatorArm64
.\gradlew.bat :sharedUi:compileTestKotlinIosArm64 :sharedUi:compileTestKotlinIosSimulatorArm64
```

Os testes comuns verificam criação/edição/exclusão, flags futuras, parcelas,
rateios, erro/nova tentativa, envios repetidos, cancelamento, expiração, troca de
mês, totais efetivos, busca, filtros e limpeza do workspace ao bloquear.
O teste com Ktor MockEngine percorre editor → repositório → transporte JSON,
incluindo Bearer, campos, flags, queries e recarga. Usa domínio e dados sintéticos;
isso não é validação do servidor real.

O host Xcode e sua configuração não precisam de outra tela Swift nem de um
novo plugin. No Mac, configurar `Local.xcconfig`, compilar o scheme `SobraAi`,
executar testes Native e conferir: criar/editar/excluir renda no mês atual e
próximos, três tipos de despesa, rateios, troca de origem dividida/simples,
categoria criada no formulário, pagamento, adiantamento recebido/enviado/retirado,
perda de conexão, sessão expirada durante gravação, bloqueio em segundo plano,
temas, teclado, fonte ampliada e VoiceOver.

Não há etapa adicional de implementação prevista para os fluxos acima antes do
build Apple. Compilar KLIB no Windows não compila Swift nem faz link/execução do
app. Build, execução e validação visual Apple dependem de Mac/Xcode; não foram
substituídos por testes JVM. A etapa seguinte implementa assistente, relatórios,
perfil e formulários de cadastro/recuperação em
[kmp-funcionalidades-ios.md](kmp-funcionalidades-ios.md). A proteção Apple para
enviar códigos permanece uma dependência externa documentada.

## Evidências — 19/10/2026

- `harnessVerify :app:assembleDebug` concluído: 44 testes do app, 30 de `shared`
  e 43 de `sharedUi`, sem falhas ou testes ignorados nos relatórios JVM.
  Lint debug e APK debug aprovados, com reaproveitamento de tarefas inalteradas.
- Fontes e testes KLIB compilados para `iosArm64` e `iosSimulatorArm64`.
  Testes Native não executados no Windows; Swift e link do framework pendentes.
- SHA-256 de 68 fontes do front-end Android sem alteração; recursos e Manifest
  também preservados. Nenhum emulador ou aplicativo iniciado pelo agente.
- XML Apple, ausência de dependências Android/JVM em `sharedUi/commonMain`,
  caminhos da documentação e `git diff --check` revisados.
- Trabalho mantido na `main`, sem commit, push, mudança de versão ou publicação.
