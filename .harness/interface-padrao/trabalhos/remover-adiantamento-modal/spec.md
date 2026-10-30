# Remover adiantamento no modal — v2 aceita

## Pedido e aceite
Em 30/10/2026, usuário pediu transferir a remoção dos detalhes para o modal de alterar adiantamento. Após avaliar versões compactas, aceitou explicitamente a primeira opção: “a primeira opção tava boa, pode fazer a primeira opção, como botão maior msm”. Referência aceita: C:/Users/kaiqu/.codex/visualizations/2026/10/22/01a164b2-8977-7860-9fd3-2462f4185024/advance-modal/proposta-v2.png. Representação estática, não execução do aplicativo.

## Mapeamento vigente
| Bloco | Fonte/reuso | Preservar | Alteração |
| --- | --- | --- | --- |
| Modal/data | AdvanceExpenseDialog/AlertDialog e DateRange | fundo background, Roboto, textos, calendário, Cancelar/Confirmar | conteúdo rolável, botão Remover adiantamento largura total |
| Remoção | OutlinedButton existente/DangerRed; RemoveAdvanceDialog | contorno, confirmação, alvo48dp e bloqueio isUpdating | callback opcional onRemoveAdvanceClick, só supportsAdvance/isAdvanced |
| Detalhes | ExpenseDetailsDialog/AdvanceDetailsBlock | estado/data e Alterar data | retirar callback e botão Remover |
| Navegação | DespesasScreen | atualização e refresh existentes | edição permanece montada sob confirmação; cancelar preserva data, sucesso fecha ambos |

## Contratos e critérios
Nenhuma mudança de API financeira, cálculos ou Paga. Reusar regra supportsAdvance para Única/Parcelada; Fixa não ganha ação. Remover usa isAdvanced=false e advancedAt=null e não depende da nova data escolhida. Alvos acessíveis e conteúdo rolável em fonte ampliada/tela baixa; sem novo componente ou host nativo. HomeScreen usa os mesmos detalhes sem callback de remoção.

## Validação
Executar harnessVerify e regressões existentes ExpenseAdvanceEligibilityTest/FinanceWorkspaceTest. Dispositivos preparados e testados pelo usuário; não iniciar emuladores/simuladores conforme AGENTS.md. Conferir manualmente em Android/iPhone: remover/cancelar/falhar, manter data editada ao cancelar confirmação, atualização bloqueia controles, confirmar alteração de data, despesa não adiantada/Fixa sem remoção, fonte ampliada/tema claro e escuro.

Revisão estática de implementação: sem defeitos bloqueantes; visual/funcional executado pendente. HarnessVerify aprovado em 30/10/2026. Comparação visual executada nos aparelhos: pendente.
