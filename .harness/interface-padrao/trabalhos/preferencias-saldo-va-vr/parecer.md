# Revisão da proposta

04/11/2026 — fase mock, proposta v1, revisor independente
`/root/review_balance_preferences`, somente leitura. Veredito: **aprovado**,
sem bloqueios para apresentar ao usuário. Aceite humano pendente.

Examinou spec, imagem externa renderizada, ConfiguracoesRendaScreen,
SettingsToggleItem/SectionTitle, PerfilScreen, ProfileSettingsComponents,
IncomeCard, FinanceSummaryContent, Theme/Color, regras locais e skill.
Atendidos: localização após VA/VR antes de Renda Extra; composição e tokens
claros/escuros; textos independentes e escopo global; legenda no Início sem
perder aviso negativo. Estados assíncronos atendidos como especificação,
não como execução. Sugestão opcional: conferir quebra real dos títulos em tela
estreita; múltiplas linhas são permitidas.
Build, persistência, semântica e validação Android/iPhone não se aplicam à fase
mock; permanecem verificações da implementação, sem alegação de execução.

Conferência Git desta etapa: main, alterações anteriores preservadas
(`app/release/` não rastreado). Política main-only registrada em AGENTS.md,
rules.md, fluxo de trabalho, documentação do harness e registros do produto.
Nenhuma branch criada, nenhum commit/push realizado nesta etapa.

## Aceite e revisão da implementação

Aceite humano em 04/11/2026 registrado na spec, mantendo controles abaixo de
VA/VR e explicação. Implementação v1 revisada por
`/root/review_balance_preferences`: **pendente de validação executada**, sem
correções técnicas bloqueantes na inspeção.
Revisor examinou mock aceito, tela/controles, ViewModel e testes, DTOs,
datasource/repositório/Ktor, legenda, revisão financeira e consumidores de
Início/Relatórios/comparativo/exportação e hosts. Confirmou na fonte:
localização/reuso, PATCH parcial/false, preservação em falha, serialização,
guards de sessão, invalidação e ausência de recálculo financeiro local.

Durante o teste HTTP isolado, a asserção de Content-Type foi ajustada ao
OutgoingContent do MockEngine; nenhum código de produção mudou após a revisão.
Harness completo aprovado em 04/11/2026: `harnessVerify`, exit 0,
BUILD SUCCESSFUL em 6m46s, 169 tarefas (75 executadas, 94 reaproveitadas).
Incluiu testes JVM, APK debug, Lint, guarda de apresentação compartilhada e
KLIB de fontes/testes shared/sharedUi para iosArm64/iosSimulatorArm64,
além de designSystem para Android e ambos os targets Apple.
Relatórios XML dos testes novos: BalancePreferencesHttpTest 6,
BalancePreferencesViewModelTest 12, AvailableBalancePresentationTest 3;
21 testes, zero falhas/erros/ignorados. Nenhum dispositivo iniciado.
Log extenso e cópia do APK ficam na pasta temporária externa da proposta,
sem versionar artefatos. Versão do aplicativo preservada.
Permanecem pendentes: execução visual/acessibilidade Android e iPhone pelo
usuário (temas, fonte ampliada, títulos, espera/erro e privacidade), além de
build/link Xcode. Mock e inspeção não substituem essas evidências.

Após o harness, o mesmo revisor conferiu diretamente o log e os três XMLs:
validação técnica e automatizada atendidas, sem bloqueios demonstrados.
Veredito final permanece pendente somente das evidências visuais/manuais e
Apple descritas acima; não representa reprovação do código nem aprovação visual.

## Clarificação textual solicitada pelo usuário

04/11/2026, fluxo curto: revisor `/root/review_balance_preferences` aprovou a
clareza/ortografia e compatibilidade do novo texto, sem bloqueios. Confirmou
ativação/desativação, exemplo 725−600=125 sem desconto duplicado, preservação
dos dados e escopo da conta. Busca transversal em app/sharedUi/designSystem/
iosApp não encontrou outro texto equivalente ainda ambíguo. Nenhum controle,
componente, navegação ou cálculo foi alterado. A quebra real do texto ampliado
em tela estreita/fonte grande permanece para o usuário conferir.

Validação após a clarificação: `harnessVerify` aprovado, exit 0, 5m14s,
169 tarefas (32 executadas, 137 reaproveitadas). APK atualizado na pasta
temporária externa, sem alterar versão. Log `harness-explanation.log` nessa
pasta. Build Android, testes JVM, Lint e compilação KMP aprovados; execução
visual e build/link Apple permanecem pendentes conforme a política do projeto.

## Mock v2 — última seção e modal de ajuda

04/11/2026: `/root/review_balance_preferences` examinou a nova imagem externa
`proposta-v2-ajuda.png`, seção Ajuste v2 da spec, tela/componentes e AlertDialog
de Perfil/Categorias. Veredito: **aprovado**, sem bloqueios para apresentar.
Confirmou posição final após Renda Extra, resumo e “?”, modal organizado com
ativar/desativar/exemplo/preservação/escopo, composição e temas coerentes.
Alvos48dp, corpo rolável, fechamento sem mutação e acesso durante operações
estão atendidos como especificação; execução/foco/rolagem/testes são futuros.
Aceite humano da v2 pendente. Nenhum código de produção foi alterado para v2.

## Implementação v2

Aceite em 04/11/2026: “Aprovado, só deixa o modal mais bonito”. Implementada
ordem final após Renda Extra e ajuda sob demanda. Polimento: exemplo com
bodyLarge/semibold/primary em SettingsCard, espaçamentos por tokens e tema.
Revisor `/root/review_balance_preferences` conferiu fontes/mock/pedido:
sem bloqueios técnicos. Confirmou corpo rolável/Fechar fixo, ajuda sem mutações,
descrição acessível, posição final e apresentação comum sem alterar API/estado.
`harnessVerify` v2 aprovado em 04/11/2026, exit0, 3m16s, 169 tarefas (35
executadas, 134 reaproveitadas): JVM, APK, Lint, guarda arquitetural e KMP.
Log externo harness-help.log; APK atualizado na mesma pasta temporária externa,
sem alteração de versão/assinatura de release, sem commit/push.
Permanecem pendentes os testes visuais/manuais nos aparelhos
(fonte, temas, foco, rolagem, fechamento e acessibilidade) e build/link Xcode.
