# Preferências de saldo VA e VR

04/11/2026. Fluxo completo: novas ações persistentes e campos de contrato comuns.
Localização decidida pelo usuário: “Em Configurações de Renda (recomendado)”.
Mock v1 aceito em 04/11/2026: “Ficou legal, deixa embaixo de VA e VR essas opções
e tem que ter um texto explicando o que essa funcionalidade faz, está no caminho certo”.
Preservar a explicação e localização mostradas. Backend externo publicado; referência de contrato:
`../App-Financeiro-Back-End/docs/handoffs/preferencias-saldo-va-vr.md`.
Checkout main; não criar branches/worktrees. Preservar app/release não rastreado.

## Proposta v1 e fontes

Mock externo: C:/Users/kaiqu/.codex/visualizations/2026/10/22/01a164b2-8977-7860-9fd3-2462f4185024/balance-preferences/proposta-v1.png.
Representação estática derivada de fontes; sem execução em dispositivos.

| Bloco | Referência e reuso | Diferença |
| --- | --- | --- |
| Página de rendas | sharedUi/.../feature/perfil/components/ConfiguracoesRendaScreen.kt; SettingsTopBar, LazyColumn, IncomeCard, SettingsCard, MonthSelector existentes | Inserir bloco após VR e antes de Renda Extra; manter seletor, ordem e formulários mensais |
| Preferências | designSystem/.../feature/perfil/ProfileComponents.kt: SettingsToggleItem, consumidor PerfilScreen, tema Theme.kt/Color.kt | Compor dois itens já existentes, título Saldo disponível e explicação global; nenhuma API visual nova |
| Saldo no Início | designSystem/.../feature/home/components/FinanceSummaryContent.kt: HighlightSummaryCard | Somente legenda: “Saldo livre no mês · sem VA”, “· sem VR” ou “· sem VA e VR”; manter aviso negativo antes da exclusão; permitir quebra sem corte |

Título dos controles: Excluir VA do disponível / Excluir VR do disponível.
Subtítulos: Vale-alimentação / Vale-refeição. Ambos desligados por padrão.
Explicação original do mock: “Vale para todos os meses. Só o restante é excluído;
os benefícios continuam nos cartões.” Substituída pela clarificação textual
autorizada em 04/11/2026, descrita abaixo.
Mock mostra claro/escuro, um controle ligado e outro desligado e exemplos do
card no Início, com valores fictícios. Imagens são aproximações das métricas;
implementação deve reutilizar os componentes completos e ícones Material locais.

## Contrato, estado e critérios

- GET /api/users/balance-preferences autenticado; PATCH parcial somente campo
  alterado, incluindo false explícito. Resposta devolve ambos os campos.
- Flags exclude_va_from_available/exclude_vr_from_available independentes por
  usuário, todos os meses/dispositivos; ausentes em resumo legado = false.
- Carregamento separado das rendas; não gravar defaults por falha de leitura.
  Controles indisponíveis até GET válido; indicador compacto na leitura/gravação,
  AppDataErrorBanner e tentativa explícita. Falha preserva valores confirmados;
  gravação bloqueia duplicação e nunca repete PATCH automaticamente.
- Salvar com sucesso invalida revisão financeira e recarrega resumo. Manter
  total_geral_disponivel da API, sem recálculo/subtração local. Revisar Início,
  Relatórios, comparativo, exportação e consumidores dos DTOs/adaptadores nativos.
- Troca de conta descarta estado/resultado antigo. Troca de mês não redefine
  preferência nem mostra dados de outro período. Cancelamento propagado.
- Cartões, fontes, recebido/gasto e percentuais integrais; restante negativo
  respeitado. Testar quatro combinações, PATCH false/omissão, falha, concorrência,
  troca de sessão e mês, valores integralmente consumidos/negativos e rateios.
- Manter tela rolável, texto multilinha, 48dp, semântica Switch, Roboto, privacidade,
  tema claro/escuro. Validação visual em aparelhos pelo usuário, sem iniciá-los.
- Após aceite e implementação: harnessVerify e APK debug para o usuário. Build/link
  Xcode e execução Apple dependem de Mac/iPhone. Não alterar versão/commit/push.

## Fluxo

Perfil → Configurações de Renda → rolar até VA/VR → alternar opção → aguardar
confirmação → voltar ao Início com resumo atualizado. Sem nova tela ou modal.

## Resultado da implementação

Implementados DTO/datasource/GET-PATCH/repositório comuns, estado retido em
BalancePreferencesViewModel, controles existentes na posição aceita e legenda
derivada das flags do resumo. Sucesso invalida a revisão financeira que já é
observada em Início, Despesas e Relatórios/comparativo. Todos preservam os
valores autoritativos do servidor; transporte nativo não precisou mudar.
Fonte de renda e PATCH de preferências continuam separados.

`harnessVerify` aprovado em 04/11/2026; 21 testes novos aprovados, APK/Lint e
compilação KMP concluídos. APK debug separado para o usuário testar, sem
alterar versão. Sem commit/push. Revisão técnica independente sem bloqueios;
fidelidade executada, acessibilidade Android/iPhone e Xcode pendentes.

## Clarificação textual posterior — 04/11/2026

Handoff do chat Web autorizado expressamente pelo usuário: a explicação estava
vaga. Aplicado fluxo curto da skill Interface Padrão, somente texto no mesmo
Text rolável, sem novos componentes, navegação, alteração de controles ou cálculo.
A explicação agora descreve ativar (retira restante do Total Geral Disponível),
desativar (reintegra), exemplo VA725/gasto600/restante125 sem desconto duplicado,
preservação de rendas/despesas/cartões/Total Recebido/Gasto e escopo de todos os
meses/dispositivos da conta. Nenhum arquivo Web/backend alterado. A diferença
textual foi solicitada diretamente; não há nova decisão de produto pendente.

## Ajuste v2 — última seção e ajuda em modal

Pedido direto posterior do usuário, 04/11/2026: Saldo disponível deve ser a
ÚLTIMA seção, depois de Renda Extra. Essa localização prevalece sobre a v1.
Pediu resumo curto e ícone “?” abrindo explicação organizada. Fluxo completo
por acrescentar modal/ação e reorganizar conteúdo. Mock v2 aprovado pelo revisor.
Aceite humano em 04/11/2026: “Aprovado, só deixa o modal mais bonito”.
Implementar composição aceita, refinando hierarquia do exemplo (cor primária,
peso/tamanho tipográfico) e espaçamentos pelos tokens existentes, sem novo fluxo.

Mock externo `balance-preferences/proposta-v2-ajuda.png` na pasta da proposta
v1: representação da tela rolada no final e modal aberto nos temas claro/escuro.

| Bloco | Reuso/referência | Diferença necessária |
| --- | --- | --- |
| Ordem da lista | LazyColumn e IncomeCard da ConfiguracoesRendaScreen | Mover bloco completo (título, aviso, dois controles) para depois de Renda Extra, sem mudar cards ou cadastro |
| Cabeçalho de saldo | SectionTitle existente em Box com weight, IconButton de 48dp, Icons.AutoMirrored.Filled.HelpOutline já usado em PerfilScreen/AjudaScreen | “?” ao lado do título, descrição acessível “Como funciona o saldo disponível”; não mexer na API do componente |
| Resumo | Text bodyMedium/onSurfaceVariant já existente | “Ative para tirar o restante do benefício do total geral.” |
| Modal de ajuda | AlertDialog da área (PerfilScreen/CategoriasScreen), background/onBackground, raio24, TextButton azul; SettingsCard compacto já existente para exemplo | Título “Como funciona”, texto de ativar/desativar, exemplo em card e notas de preservação/escopo; nenhuma API HTTP ou controle adicional |

Conteúdo do modal: “Ao ativar, o restante do VA ou VR sai do Total Geral
Disponível. Ao desativar, ele volta a compor o total.” Exemplo com VA:
“R$ 725 − R$ 600 gastos = R$ 125 restantes”; “Só os R$ 125 saem do disponível.
Os R$ 600 não são descontados novamente.” Rodapé: “Não apaga rendas ou despesas
nem altera os cartões, o Total Recebido e o Total Gasto.” e “Vale para todos os
meses e dispositivos da conta.” Botão Fechar, toque fora e voltar fecham apenas
a ajuda, sem salvar/alterar preferências. Reabrir mostra a mesma informação.

Manter corpo do modal rolável para fonte ampliada/tela baixa e título/Fechar
acessíveis, alvos48dp, temas e semânticas. Indicadores/erros/PATCH/parcialidade,
sessão, revisão financeira e valores/negativos da API permanecem iguais.
Permitir abrir ajuda durante leitura/salvamento: é informativa e não dispara
requisições. Não esconder os avisos existentes nem alterar controles ao fechar.
Implementação comum Android/iOS; não tocar Web/backend. Após aceite, verificar
abertura/fechamento, ordem final, rolagem/foco, temas, fonte grande e estados;
harnessVerify e APK atualizado. Visual e acessibilidade executados pelo usuário.

## Versão e envio autorizados

04/11/2026: usuário autorizou subir a versão Android, fazer commit e push.
Android atualizado de 1.5.2/152 para 1.5.3/153, sem alterar a versão Apple.
`harnessVerify` aprovado após o incremento (1m39s, 169 tarefas, 31 executadas
e 138 reaproveitadas); log externo harness-153.log. APK debug atualizado.
Commit/push somente na main; app/release preexistente fica fora do commit.
Não houve autorização de publicação em loja. Visual e Xcode seguem pendentes.
