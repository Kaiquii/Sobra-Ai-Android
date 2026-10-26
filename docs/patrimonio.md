# Patrimônio — FGTS e investimentos

Patrimônio acompanha saldos atuais informados manualmente. Os registros são do
usuário autenticado e persistem no backend; não entram em receitas, despesas,
saldo mensal, fontes de pagamento, relatórios, exportações ou assistente IA.
Atualizar substitui o saldo, sem calcular aportes, rendimentos ou histórico.

## Interface única

- Atalho flutuante no Início: botão azul de 48 dp, somente com ícone de carteira
  de 24 dp, sem texto visível e com descrição acessível “Abrir patrimônio”.
  À direita e próximo da barra, preservando o “+”, as quatro abas e o espaço
  medido ao fim da lista.
- Entrada adicional em Perfil > Finanças > Patrimônio, com subtítulo “FGTS e
  investimentos”. Ambos abrem a mesma tela e voltam ao local de origem.
- A tela comum usa resumo compacto com total geral e composição FGTS/investimentos,
  sem introdução nem títulos repetidos. Cards compactos mostram saldos, categoria
  e última atualização. O toque no card abre diretamente o painel de atualização;
  a indicação “Atualizar” torna a ação explícita. Cadastro de FGTS e investimento
  tem acesso direto também nos estados vazios.
- Cadastro/edição usam um painel inferior rolável, com campos identificados,
  teclado seguinte/concluir e botão principal de salvar ocupando a largura.
  O componente comum `AppModalBottomSheet` protege os formulários de FGTS, novo
  investimento e edição enquanto o teclado está aberto: deslize, toque na alça,
  toque fora e voltar não descartam o rascunho. Com o teclado fechado, o deslize
  volta a fechar o painel. A rolagem dos campos e o botão “X” são preservados;
  durante o salvamento, os bloqueios existentes continuam impedindo o fechamento.
  Exclusão fica no próprio editor e pede confirmação; não exige outra tela.
- FGTS ausente oferece cadastro; saldo zero cadastrado continua um registro que
  pode ser editado ou removido. Investimento tem nome, categoria do catálogo e saldo;
  as categorias de investimentos são independentes das categorias de despesas.
- Exclusões exigem confirmação. Formulários aceitam zero, explicam substituição
  do saldo e preservam o erro para correção. Valores exibidos usam FinancialValue
  e respeitam a privacidade comum.
- Falha inicial mostra erro e totais indisponíveis, sem zeros fictícios. Com dados
  anteriores, preservar a consulta com aviso de desatualização e retry. Falha de
  autorização remove dados protegidos; troca de conta descarta os da conta antiga.

## Categorias e compatibilidade

O formulário usa AppSelectField com IDs retornados pela API e oferece Nova categoria
(com seleção do ID criado) e Gerenciar categorias. Categorias padrão são marcadas
como protegidas e não oferecem edição/exclusão. O gerenciador mantém cabeçalho,
fechar, descrição e Nova categoria fixos, com rolagem apenas da lista. Status
longos ficam em uma região limitada, mantendo os controles acessíveis. Em
janelas baixas, a frase de ajuda opcional é ocultada para reservar espaço à lista.
Categorias pessoais podem ser
criadas, renomeadas e excluídas; a API bloqueia exclusão em uso, preservando os
investimentos e orientando trocar a categoria antes. Não há IDs fixos no app.

Após renomear/excluir, recarregar catálogo e Patrimônio para atualizar os rótulos,
sem alterar saldos, datas ou a revisão financeira mensal. Categoria removida por
outra sessão força consulta nova, sem reaproveitar o catálogo anterior por 60 s.

**Publicação confirmada pelo backend em 26/10/2026:** nova API e migration 7
publicadas, com catálogo autenticado e smoke HTTP/HTTPS registrados em
App-Financeiro-Back-End/docs/publicacao-categorias-investimentos.md. A evolução
seguinte do backend (migration 8) inclui CDI como padrão protegido; o app recebe
o catálogo completo sem fixar nomes, quantidade ou IDs. O app usa a
mesma base configurada e IDs da API, sem migração ou deploy pelo frontend.

A compatibilidade por texto permanece somente quando GET do catálogo retorna
404. Esse resultado não é reutilizado na reentrada por 60 s: voltar a Patrimônio
ou abrir novamente o editor consulta o catálogo, reutilizando uma leitura idêntica
que ainda estiver em andamento. Após GET 200, supported=true substitui o estado
legado e as entradas normais voltam a aproveitar o cache comum. Não há polling nem
repetição automática de gravações. Falhas de rede e HTTP 401/403 não simulam
catálogo vazio nem ativam a compatibilidade.

A publicação/smoke do servidor são evidências do backend. A verificação do app
usa fakes/MockEngine; execução e comparação visual no Android/iPhone continuam
pendentes de teste manual pelo usuário.

## Código e API

- Modelos e serialização: shared/.../core/network/PatrimonyModels.kt.
- Contrato injetável e transporte: shared/.../core/data/PatrimonyDataSource.kt.
- Estado, ações e tela: sharedUi/.../feature/patrimonio/, incluindo
  InvestmentCategoriesViewModel e InvestmentCategoriesDialog.
- Catálogo injetável: shared/.../core/data/InvestmentCategoriesDataSource.kt,
  implementado pelo mesmo SharedPatrimonyRepository e cliente dos dois hosts.
- Atalho visual: designSystem/.../core/designsystem/components/PatrimonyAccessButton.kt.
- Painel inferior: designSystem/.../core/designsystem/components/AppModalBottomSheet.kt,
  compartilhado também por filtros de despesas, exportação e histórico do Assistente.
- UiServices recebe PatrimonyDataSource nos dois hosts, compartilhando os clientes
  HTTP e a configuração de URL existentes. Não há implementação nativa de tela.

Rotas, autenticadas pelo Bearer atual:

| Método | Caminho | Conteúdo |
| --- | --- | --- |
| GET | api/patrimony/ | fgts, investments, total_fgts, total_investments, total_patrimony |
| PUT | api/patrimony/fgts | amount; cria ou substitui o FGTS |
| DELETE | api/patrimony/fgts | Remove o FGTS informado |
| POST | api/patrimony/investments | name, category_id, amount; category por texto no legado |
| PATCH | api/patrimony/investments/:id | name, category_id, amount; category por texto no legado |
| DELETE | api/patrimony/investments/:id | Remove o investimento |
| GET | api/patrimony/investment-categories | total e categories com id, name, is_default |
| POST | api/patrimony/investment-categories | name; retorna data com categoria criada |
| PATCH | api/patrimony/investment-categories/:id | name; retorna data com categoria renomeada |
| DELETE | api/patrimony/investment-categories/:id | Remove categoria pessoal sem investimentos |

POST/PATCH/PUT leem o envelope data; DELETE lê message. Após sucesso, recarregar
GET para apresentar os totais calculados pelo servidor. Não emitir revisão em
FinanceDataChanges. Uma resposta perdida de gravação fecha o editor com aviso e dispara apenas GET
para conferência; bloquear novas gravações durante a consulta e se ela falhar,
e não repetir POST/PUT/PATCH/DELETE automaticamente. O usuário deve conferir a
lista antes de abrir um novo cadastro.

GET usa a mesma recuperação comum das outras telas: até três novas tentativas
espaçadas após falhas de conexão (500, 1.500 e 3.000 ms), sem repetir erros HTTP
nem timeouts que já aguardaram seu limite. Reabrir uma leitura ainda em andamento
reutiliza a mesma requisição. Isso trata quedas transitórias, inclusive no retorno
ao app, sem ocultar falhas persistentes ou alterar os limites de 30/90 segundos.

PatrimonyAmount usa centavos Long, sem Double. Entradas aceitam pt-BR com grupos
de milhar e até duas casas; valores individuais vão de zero a 9.999.999.999,99.
O JSON emite número decimal normal, sem aspas nem expoente. Totais recebidos podem
ser maiores que o limite individual. Nomes são aparados, com 1–120 caracteres;
nome da categoria tem 1–80. Enviar somente category_id ou category, nunca ambos.
Não enviar id, user_id, is_default nem datas no corpo.
Datas RFC3339 são convertidas em instantes antes da apresentação no horário local.
Receitas e despesas preservam seus modelos e contratos monetários anteriores.

## Validação

Executar harnessVerify para testes JVM, APK, Lint e compilação KMP dos dois
targets Apple. Os testes comuns cobrem zero/centavos/limites/JSON, CRUD HTTP,
reabertura, recarga de totais, invariância da revisão mensal, falhas/retry,
resposta perdida, bloqueio e troca de conta. Testes automatizados usam dados
sintéticos e fakes/MockEngine, sem alterar dados reais do servidor.
InvestmentCategoriesViewModelTest e PatrimonyCategoriesHttpTest cobrem CRUD do
catálogo, proteção, seleção/gravação por ID, conflitos, exclusão em uso, fallback
GET 404, falhas de autenticação, resposta perdida e isolamento entre contas.

A conferência manual é do usuário: cadastrar FGTS zero, atualizar saldo, criar,
editar e excluir investimento; com a nova API, criar/selecionar/renomear/excluir
categorias pessoais, conferir padrões protegidos e exclusão em uso; reabrir o app,
confirmar totais e resumo do mês
inalterado; testar temas, fonte ampliada, telas estreitas, teclado, privacidade,
atalho e retorno para Início/Perfil em Android e iPhone. Build/link Xcode e
execução Apple continuam validações separadas, dependentes de Mac/iPhone.

Nos editores de FGTS, novo investimento e edição, abrir o teclado e deslizar o
painel: ele deve permanecer aberto, sem perder os campos. Conferir também toque
na alça, toque fora e voltar com o teclado aberto. Fechar o teclado e deslizar
novamente: o painel deve fechar normalmente. O “X” continua sendo uma ação
explícita de fechamento; ao salvar, nenhum gesto deve interromper a operação.
Repetir os cenários equivalentes nos filtros de despesas, na exportação e no
histórico do Assistente, incluindo o bloqueio enquanto a exportação estiver em
andamento. Esses gestos exigem conferência manual nas duas plataformas.
