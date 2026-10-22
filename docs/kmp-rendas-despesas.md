# Rendas e despesas — apresentação única Android/iOS

Todas as telas usam a implementação completa migrada do Android, em
`sharedUi/src/commonMain/kotlin/com/example/appfinanceiro/`.
Ambos os hosts abrem `SobraAiApp(services)`; não há editores iOS alternativos.

| Fluxo | Fonte comum |
| --- | --- |
| Visão mensal, saldos, rendas e despesas | `feature/home/HomeScreen.kt`, `HomeViewModel.kt` e `components/ResumoFinanceiroSection.kt` |
| Criar/editar/excluir renda | `feature/home/components/income/IncomeEditorDialog.kt`, `IncomeDeleteDialog.kt`; configurações em `feature/perfil/components/ConfiguracoesRendaScreen.kt` |
| Lista, filtros, detalhes, pagamento, exclusão e adiantamento | `feature/despesas/DespesasScreen.kt`, `DespesasViewModel.kt` e `components/AdvanceExpenseDialogs.kt` |
| Criar/editar despesa e categoria no formulário | `feature/despesas/components/NovaDespesaScreen.kt`, `EditarDespesaScreen.kt` |
| Rateios e seletores | `feature/despesas/components/PaymentSplitSection.kt`, `FormComponents.kt` e `core/designsystem/components/AppDatePickerDialog.kt` |
| Acesso aos dados editáveis | `core/data/FinanceActionsViewModel.kt`, com `SharedFinanceRepository` injetado |

Preservar `Double`, unidade monetária, valores de domínio, recorrência, flags
futuras, notas, parcelas e regras de pagamento/adiantamento da API. Lista agendada
e período efetivo têm propósitos distintos; não somar duas vezes o mesmo ID.
Dias dos seletores são UTC; o payload de despesa mantém a meia-noite com fuso
local e o adiantamento mantém a data civil. Parsing/fuso ficam nos adaptadores;
as decisões dos formulários e seus componentes são comuns.

Privacidade, filtros, cancelamento e tratamento de expiração devem valer nas
duas plataformas. Não introduzir regras financeiras específicas no host.
Os contratos em `shared/.../FinanceDataSources.kt` aceitam fakes nos testes.

Validar com `harnessUnit` e `harnessVerify`. No Mac, complementar com build/link
Xcode e a comparação visual conduzida pelo usuário. Testes Native ou aparelhos
somente quando explicitamente solicitados. Consulte [a integração](kmp-compose-ios.md)
e [as regras visuais](frontend-rules.md).
