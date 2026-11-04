# VA e VR no aplicativo

VA (vale-alimentação) e VR (vale-refeição) são fontes independentes. Seguem o
cadastro mensal de Salário e Adiantamento: criação com `type=Fixa` no mês/ano
selecionado, edição pelo ID com `update_future` e exclusão pelo ID com
`delete_future`. A API cria os 60 meses e aplica as alterações futuras, inclusive
na virada do ano; o aplicativo não replica lançamentos ou calcula recorrência.
`repeat_future` continua exclusivo de Renda Extra.

Não existe carteira conjunta, transferência automática entre meses ou bloqueio
por saldo negativo. Os totais recebidos/gastos incluem os benefícios; o saldo
disponível respeita as preferências abaixo. Não somar nem subtrair VA/VR outra
vez no cliente. O aplicativo lê `va`, `vr`, `total_gasto_va`,
`total_gasto_vr`, `restante_va` e `restante_vr`, com zero como default para
respostas antigas. O restante é o valor recebido da API, inclusive se negativo.

A origem VA/VR não altera a elegibilidade para adiantar: somente despesas Única
ou Parcelada permitem adiantamento. Fixa pode ser marcada como paga, mantendo o
gasto no mês previsto, e não oferece ações de adiantamento.

## Exclusão do saldo disponível

Em Configurações de Renda, como última seção depois de Renda Extra,
dois `SettingsToggleItem` permitem excluir independentemente o restante de
cada benefício do disponível geral. A tela mantém resumo curto e um “?” que
abre ajuda em AlertDialog comum, com exemplo destacado em SettingsCard,
corpo rolável e Fechar acessível. A explicação informa que ativar retira o
restante do Total Geral Disponível e desativar o inclui novamente. Traz o exemplo
VA R$ 725 menos R$ 600 gastos = R$ 125 restantes, sem descontar o gasto outra vez.
Também esclarece que não apaga rendas/despesas, não altera cartões nem Total
Recebido/Gasto e vale para todos os meses/dispositivos da conta.
Os controles começam desligados para quem não optou pela exclusão; não inferir
a preferência a partir de rendas cadastradas.

`GET /api/users/balance-preferences` retorna diretamente
`exclude_va_from_available` e `exclude_vr_from_available`. O `PATCH` envia
somente a flag alterada, inclusive `false` explícito para desligar; omitir a
outra preserva sua escolha. Ambos usam autenticação normal. As preferências
pertencem ao usuário e valem para todos os meses/dispositivos.

`BalancePreferencesViewModel` retém a leitura na navegação, separada das rendas,
e só publica escolhas confirmadas pelo servidor. Falha de GET bloqueia edição
até uma tentativa explícita bem-sucedida, sem gravar defaults; falha de PATCH
preserva o estado confirmado. Leitura/gravação não concorrem, toques repetidos
são bloqueados e PATCH não é repetido automaticamente. Troca de sessão cancela
operações antigas e impede publicar resultado ou invalidar a conta nova.

Sucesso incrementa `FinanceDataChanges.revision`, invalidando os resumos
retidos do Início e Relatórios, inclusive ao voltar de Configurações de Renda.
O app usa `total_geral_disponivel` fornecido pela API, sem recalcular exclusões.
Com VA de R$ 725 e gasto de R$ 600, o cartão mantém R$ 125; a exclusão retira
apenas esses R$ 125 do disponível. O restante pode ser zero ou negativo.
Recebido, gasto, cartões, fontes, percentuais e exportações continuam integrais.

As flags também vêm no resumo, com `false` como default apenas para respostas
legadas sem os campos. A legenda do saldo no Início identifica VA, VR ou ambos
excluídos e preserva o aviso de saldo negativo. O GET específico exige os dois
campos: uma resposta incompleta não é uma confirmação de preferências desligadas.

## Pontos compartilhados

- `shared/.../core/network/PaymentSources.kt`: fontes canônicas, rótulos e seleção
  de renda estrita por mês/ano, usada pelos dois hosts.
- Configurações de Renda e Resumo Financeiro: cadastrar, editar e excluir VA/VR
  com os mesmos componentes e opções das rendas existentes.
- Nova/Editar Despesa: VA/VR em origem única ou divisão com as demais fontes,
  nos tipos Única, Fixa e Parcelada; payload usa `payment_source` ou
  `payment_splits`, nunca os dois. Divisões não repetem fontes e somam o total.
- Início e Despesas: filtros reconhecem VA/VR também em pagamentos divididos.
- Relatórios, comparativos, compromissos, exportações e Assistente: preservam
  fontes e totais devolvidos pelo servidor, sem recalcular benefícios localmente.
- Alterações de renda invalidam Início/Despesas/Relatórios pelo evento comum de
  revisão, incluindo as fontes antigas. Não emitir sucesso ou invalidar por falha.

## Verificação

`BenefitsTest` cobre payloads recorrentes, seleção de dezembro/janeiro, IDs e
escopo de edição/exclusão, fontes independentes, pagamentos divididos e filtros,
defaults, preservação dos saldos/totais da API e erros de gravação. Os testes de
contrato HTTP em `ApiContractTest` verificam JSON e parâmetros realmente enviados.
Não substituem a integração do servidor que cria/edita/exclui os meses futuros.

`BalancePreferencesHttpTest` verifica o contrato GET/PATCH, omissão versus
false, respostas inválidas/erros, flags do resumo e ausência de retry da gravação.
`BalancePreferencesViewModelTest` verifica confirmação, erros, concorrência,
invalidação e sessão. `AvailableBalancePresentationTest` cobre as quatro
legendas, aviso negativo, valores autoritativos após chegada das rendas e troca
de mês/ano, incluindo benefícios consumidos e negativos.

Conferir manualmente as quatro combinações de interruptores, persistência ao
reabrir/alternar dispositivo, mudanças de mês/conta, falhas e retorno ao Início.
O bloco permanece disponível sem renda VA/VR cadastrada e não altera o mês do
seletor nem replica valores mensais. Incluir tema claro/escuro, tela estreita,
fonte ampliada, TalkBack e VoiceOver. Execução real não é comprovada pelos testes.

Executar `harnessVerify`. O usuário confere em Android e iPhone: cadastro em
dezembro e consulta em janeiro; edição/exclusão do mês e dos futuros; VA sem
alterar VR; rateio VA/VR/Salário; valores negativos; adiantamento no período
efetivo; marcar paga sem novo desconto; atualização ao alternar abas; temas,
fonte ampliada, teclado e conectividade. Sem iniciar dispositivos automaticamente.
No Mac, complementar com build/link Xcode; KLIB no Windows não prova execução iOS.
