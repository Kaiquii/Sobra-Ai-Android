# VA e VR no aplicativo

VA (vale-alimentação) e VR (vale-refeição) são fontes independentes. Seguem o
cadastro mensal de Salário e Adiantamento: criação com `type=Fixa` no mês/ano
selecionado, edição pelo ID com `update_future` e exclusão pelo ID com
`delete_future`. A API cria os 60 meses e aplica as alterações futuras, inclusive
na virada do ano; o aplicativo não replica lançamentos ou calcula recorrência.
`repeat_future` continua exclusivo de Renda Extra.

Não existe carteira conjunta, transferência automática entre meses ou bloqueio
por saldo negativo. Os totais gerais do resumo já incluem os benefícios; não
somar VA/VR outra vez. O aplicativo lê `va`, `vr`, `total_gasto_va`,
`total_gasto_vr`, `restante_va` e `restante_vr`, com zero como default para
respostas antigas. O restante é o valor recebido da API, inclusive se negativo.

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

Executar `harnessVerify`. O usuário confere em Android e iPhone: cadastro em
dezembro e consulta em janeiro; edição/exclusão do mês e dos futuros; VA sem
alterar VR; rateio VA/VR/Salário; valores negativos; adiantamento no período
efetivo; marcar paga sem novo desconto; atualização ao alternar abas; temas,
fonte ampliada, teclado e conectividade. Sem iniciar dispositivos automaticamente.
No Mac, complementar com build/link Xcode; KLIB no Windows não prova execução iOS.
