# Relatórios, assistente, perfil e autenticação no iOS

Implementação em `:sharedUi`, diretamente na `main`. O Android `:app` continua
usando seu front-end original; ele não depende de `:sharedUi`. Os contratos JSON,
repositórios, sessão e regras financeiras ficam em `:shared` para as duas
plataformas. Nenhuma tela financeira é duplicada em Swift.

## Funcionalidades

- **Relatórios:** resumo mensal, categorias, gráfico de receitas/despesas de
  1/6/12 meses com virada de ano, resumo anual, comparação entre meses,
  diferenças por categoria/origem/tipo e insights. Os valores vêm da API.
- **Compromissos parcelados:** compras ativas, parcelas, próxima ocorrência,
  cronologia/calendário mensal, horizonte de 6/12/24 meses e simulação do mês base
  como pago. A simulação só altera a consulta, não marca despesas como pagas.
  A projeção filtra séries encerradas e recalcula os totais como no Android.
- **Exportação:** sete conteúdos, PDF/Excel/CSV, meses comparados e flags de
  compromissos. O arquivo retornado pela API abre a folha nativa de compartilhar
  (incluindo Salvar em Arquivos). Arquivos temporários protegidos são removidos
  ao encerrar ou cancelar a folha. Nenhum arquivo é enviado a contatos pelo app.
- **Assistente:** nova conversa, continuidade pelo `conversation_id`, histórico,
  abrir/excluir conversa, erros, limite temporário e entrada por voz. Só o resultado
  estruturado `create_expense/status=created` recarrega os dados financeiros.
  A transcrição preenche a mensagem para revisão e **não envia automaticamente**.
- **Perfil:** editar nome/e-mail, foto/visualização/remoção, biometria por conta,
  categorias (criar/editar/excluir), acesso a rendas, ajuda/suporte e saída.
  Alterar o e-mail preserva a preferência ativa de biometria na nova identidade.
- **Cadastro/recuperação:** detalhes, código de seis dígitos, senha/repetição na
  recuperação, validação, erros, reenvio com limite por e-mail e conclusão pelos
  endpoints existentes. O envio de códigos fica indisponível até a integração
  Apple do backend; há opção para informar código válido já recebido.

O usuário autorizou deixar o protocolo Apple de envio como pendência. Os requisitos
e o ponto de integração estão em [pendencias-backend-ios.md](pendencias-backend-ios.md).
Não há prova fictícia, pedido desprotegido ou remoção de Play Integrity no Android.
A consulta de versão Apple também requer confirmação de contrato do servidor.

## Arquitetura e ciclo de vida

`FinanceWorkspace` cria `WorkspaceFeatures` com o escopo da sessão. `FeatureQuery`
mantém juntos os filtros e o resultado carregado, descarta respostas canceladas e
conserva dados anteriores identificados quando uma nova consulta falha.
`FeatureAction` bloqueia toques duplicados e mantém erros visíveis. Escritas não
têm nova tentativa automática. Logout/bloqueio cancela as operações da sessão.

`ReportsPresenter`, `AssistantPresenter`, `ProfilePresenter` e
`AccountAuthPresenter` contêm a lógica comum e recebem contratos injetáveis.
`ProfileDataSource` complementa os contratos existentes sem mudar
`FinanceDataSources.kt`. `FinanceFiles.kt` valida formato/tamanho dos downloads,
JPEG do upload e origem do avatar. 401 da API expira a sessão; revogação mantém o
evento comum. Uma falha de CDN não é tratada como expiração da conta.

`IosNativeFeatures` implementa `NativeFeatures`: UIKit para galeria/compartilhar,
Speech + AVAudioEngine para ditado pt-BR e abertura de links de suporte por ação
do usuário. A foto é redimensionada até 2048 px, orientada e recodificada em JPEG;
metadados originais não são enviados. Permissões de galeria, microfone e fala têm
descrições em português no Info.plist. Negação de permissão mantém entrada manual.
Ditado encerra em até 45 segundos ou por ação/cancelamento/segundo plano.

Face ID/Touch ID usa `LocalAuthentication`, conforme o sensor disponível. Ativar
proteção exige autenticação real; a preferência passa a valer no mesmo ciclo de
sessão e bloqueia o painel no segundo plano. Sem autenticação aceita, o app mantém
o bloqueio e permite encerrar a sessão para entrar com senha. Face ID não substitui
o login inicial nem a integridade Apple exigida para envio de código.

Privacidade oculta dados de relatórios e a conversa (inclusive a composição da
mensagem). O host cobre a cena inativa. Áreas seguras, teclado, rolagem, tema e
semânticas acompanham os componentes comuns; aprovação visual requer aparelho.

## Validação e próximos passos

Comandos, JDK 21 e configuração do host em [kmp-compose-ios.md](kmp-compose-ios.md).
Usar `harnessVerify :app:assembleDebug`, compilar fontes e testes KLIB para
`iosArm64` e `iosSimulatorArm64`. Testes usam fakes e MockEngine, sem backend real.

No Mac: build do scheme SobraAi, testes Native e execução dos seguintes fluxos:

1. Login/restauração, oferta e ativação/cancelamento Face ID/Touch ID, alteração da
   preferência no perfil, segundo plano, sessão expirada e saída.
2. Relatórios, virada de ano, meses rápidos, falha de rede, comparação, compromissos
   e exportação de todos os formatos, inclusive cancelamento e iPad/popover.
3. Conversas, confirmação de despesa, atualização dos saldos, histórico/exclusão,
   quota, voz/negação de permissões e revisão antes de enviar.
4. Perfil/e-mail, seleção/remoção de foto, categorias e suporte; teclado, fonte
   ampliada, temas, rotação, privacidade e VoiceOver.
5. Formulários de código com servidor de teste; ciclo completo somente após
   implementar a pendência do backend Apple.

KLIB no Windows não compila Swift, não faz link do framework nem executa UIKit,
Keychain, Speech ou Face ID. Esses passos exigem Mac/Xcode/iPhone; nenhuma
aprovação visual, execução real Apple ou publicação é inferida dos testes JVM.

## Evidências — 19/10/2026

- `harnessVerify :app:assembleDebug` aprovado. Relatórios XML: **152 testes JVM**
  (44 no app, 37 em `shared`, 71 em `sharedUi`), zero falhas/erros/ignorados.
  As tarefas inalteradas puderam reutilizar resultados do Gradle.
- Lint debug concluído sem erros; o relatório ainda contém 51 warnings e cinco
  hints. Não foram removidas proteções ou alterada a interface Android para
  silenciar esses apontamentos.
- `compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`,
  `compileTestKotlinIosArm64` e `compileTestKotlinIosSimulatorArm64` de `sharedUi`
  concluídos. **Testes Native não executados** neste Windows.
- SHA-256 dos 68 fontes de front-end Android idêntico à linha de base; recursos
  e Manifest também sem diferenças. A branch continua `main`.
- XML do Info.plist/scheme revisado. Nenhuma dependência Android/JVM nos fontes
  comuns de `shared`/`sharedUi`. Whitespace e links locais revisados.
- Novos testes cobrem arquivos/limites/origem de token, filtros/virada de ano,
  projeção de parcelas, conversas/quota/voz tardia, categorias/foto/identidade,
  autenticação e respostas de perfil/biometria após logout.
- Nenhum backend real, emulador, app instalado, teste instrumentado, release,
  commit, push ou publicação foi usado para produzir essas evidências.

Não há outra funcionalidade cliente prevista nesta etapa antes do build Apple.
O envio de códigos depende da integração de backend autorizada como pendência;
quando o contrato chegar, também será necessário implementar seu adaptador
nativo. A consulta de versão Apple precisa de confirmação do servidor. Esses
itens estão registrados separadamente, sem afirmar que somente compilar o iOS
resolve dependências externas.
