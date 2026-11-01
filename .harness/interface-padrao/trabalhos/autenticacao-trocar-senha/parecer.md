# Parecer — fase mock

## Implementação autorizada em 31/10/2026

Revisão independente por `/root/revisar_mock_senha`: **pendente para validação
visual/interativa real**, com revisão estática concluída sem bloqueios concretos
de código. Examinou AuthPage, AuthCodeField, AuthComponents, LoginScreen,
EsqueciSenhaScreen, RegisterScreen, GateScreen, tema e referências aprovadas.
Confirmou reuso/KMP, contratos/sessão/biometria preservados, OTP de valor único,
rolagem/insets e ações após campos. Não alterou arquivos.

Após a revisão, a sessão principal concluiu harnessVerify da versão final:
BUILD SUCCESSFUL, 2m47, incluindo APK/Lint/JVM/KLIB iOS. O resultado não comprova
fidelidade visual, teclado, seleção/colagem do OTP, foco, fonte ampliada ou
acessibilidade. Esses critérios ficam para aparelhos preparados pelo usuário,
conforme AGENTS.md. Não foram iniciados app, emulador ou simulador.

## Histórico dos mocks

V3 após correção do usuário: **aprovado para apresentação** por
`/root/revisar_mock_senha`, após examinar as duas propostas e capturas reais.
Fluxo de duas etapas confirmado. A sessão principal ainda pediu refinamento
da posição de Enviar código e da área superior, antes da entrega final.

V4: **aprovado** pelo mesmo revisor. Examinados `recuperar-senha-v4.png`,
`nova-senha-v4.png` e spec atualizada. Confirmou Enviar código logo após e-mail,
área de status e retorno nas duas telas, títulos e campos corretos, ações no
fluxo vertical, sem cortes, card externo, contador ou alegações inventadas.
Símbolos das barras do sistema variam entre imagens e são ilustrativos.
Aceite humano da v4 ainda pendente. O parecer não comprova navegação, preservação
efetiva de e-mail, teclado, foco, rolagem, acessibilidade ou execução Android/iOS.

Revisor independente: `/root/revisar_mock_senha` (skill interface-padrao).
Data: 31/10/2026. Sem edição ou execução do aplicativo.

V1: aparência inicialmente aprovada pelo revisor. A sessão principal identificou
no retorno get_screen o viewport DESKTOP 2560 × 2048, incompatível com o pedido
mobile, e solicitou correção e nova revisão. A aprovação inicial não foi usada
como aprovação desse critério.

V2: **aprovado**. Examinados `trocar-senha-v2.png`, spec atualizada, referências
aprovadas login/cadastro e fontes AuthComponents/EsqueciSenhaScreen/Color.
O revisor confirmou composição de celular 780 × 1768 sem cortes, margens
consistentes, identidade do produto, quatro campos, olhos nas senhas, retorno e
Atualizar senha. Não observou contador, validação fictícia do código, ajuda ou
garantia inventada. A proporção aproximadamente 390 × 884 é compatível com o
login aprovado. O parecer v2 substitui o v1 quanto ao viewport.

Limites: revisão da proposta estática, não da implementação. Teclado, foco,
rolagem, acessibilidade e execução Android/iOS continuam sem evidência real.
Novo aceite humano da tela Trocar senha ainda pendente; código de produção
não alterado nesta tarefa.

## Revisão do ajuste compacto — 31/10/2026

Revisor independente: revisão estática aprovada, sem bloqueios concretos.
Conferidos detalhes compartilhados por Início/Despesas, explicação Paga nos
modais de operação e cabeçalhos de recuperação/confirmação sem ícone extra.
GateScreen preservado. Visual Android/iOS e fonte ampliada dependem do teste
manual do usuário; nenhum aplicativo ou emulador foi iniciado.


Validação deste ajuste: harnessVerify concluído com BUILD SUCCESSFUL em 2m5s
(build APK debug, Lint, testes JVM e compilação KLIB iOS). APK com assinatura
verificada. git diff --check sem erros. Execução visual e Apple permanecem pendentes.
