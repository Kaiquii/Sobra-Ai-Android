# Escolha automática da IA do Assistente

## Decisão e responsabilidade

Em 28/10/2026, o usuário optou por retirar a seleção manual de modelos do
Assistente. Android e iOS mantêm a mesma tela Compose de chat, sem seletor,
nome de modelo, indicação de “Recomendado” ou diálogo de informações do modelo.
O aplicativo apresenta o SobraAi; escolher e substituir a IA é responsabilidade
do backend, sem exigir que o usuário acompanhe modelos ou limites de provedores.

O cliente não consulta catálogo nem lê/grava preferências de modelo. Não mantém
uma lista de modelos/provedores, não busca logos e não contém chave de OpenRouter.
A verificação anterior de logos foi encerrada: não há entrega de ícones pendente.

O retorno definitivo do backend confirmou a publicação do roteamento automático
em 28/10/2026 às 15:36:50 (America/Sao_Paulo), com os commits `08a611e`
(implementação) e `dbeba5f` (registro da publicação) enviados à `main`.
Essa confirmação substitui o status anterior de implementação apenas local.
O backend informou harness completo, integração PostgreSQL e smoke autenticado
em HTTP interno e HTTPS de produção aprovados, incluindo preferências antigas,
continuidade da conversa e ausência de duplicação de ações. O registro detalhado
fica em `docs/publicacao-roteamento-automatico-ia.md` no repositório do backend.
Essas evidências do servidor não substituem os testes visuais Android/iPhone.

No contrato publicado pelo backend, POST sem `model_id` ou com
string vazia sempre delega a escolha ao servidor, ignorando a preferência manual
antiga nesse fluxo sem apagar o registro nem alterar seu `updated_at`. GET/PUT
legados e override explícito por mensagem continuam compatíveis para outros
clientes, com validação de gratuidade e fallback; o app não usa essas rotas,
não redefine preferências por PUT e não envia override vazio.

O servidor reutiliza o último modelo gratuito bem-sucedido por no máximo cinco
minutos, sem renovar indefinidamente esse prazo, e remove essa prioridade após
falha ou retirada do catálogo. A rotação usa erros reais, indisponibilidade e
tempos de espera/reset fornecidos pelo provedor, com tentativas limitadas. Não há
previsão confiável documentada de cota restante por modelo em respostas bem-sucedidas.
Limites globais ou pessoais interrompem tentativas, inclusive concorrentes;
trocar modelo não reinicia cotas. Histórico e confirmações financeiras permanecem
preservados. Essas regras descrevem a evolução publicada e confirmada pelo backend.

## Chat, sessão e apresentação

`AssistantScreen.kt` e `AssistantViewModel.kt` mantêm boas-vindas, mensagens,
histórico, nova conversa, exclusão, voz, rascunho e indicação de cota. A abertura
do chat e o envio não dependem de catálogo, preferência ou reconciliação de modelo.
Não deve haver carregamento, aviso ou bloqueio de envio proveniente dessa seleção
removida.

O rascunho usa `rememberSaveable` no escopo da árvore autenticada, descartado ao
encerrar a sessão, sem guardar JWT como chave/dono em Bundle. A emissão inicial
ainda não resolvida do coletor não significa logout: voltar à rota não pode limpar
ViewModels retidos. `AssistantViewModel.bindSession` recebe observações reais da
sessão e impede que respostas canceladas ou de conta anterior publiquem dados
na conta atual.

O compositor só limpa a mensagem quando `AssistantViewModel.sendMessage` retorna
aceitação. Recusa por cota, mensagem vazia, envio/leitura em andamento ou sessão
inválida preserva o texto. A escolha/troca interna do modelo no servidor deve
preservar `conversation_id`, mensagens e contexto, sem solicitar nova conversa.

## Contrato HTTP do cliente

O POST `api/assistant/chat` mantém `message` e `conversation_id` existentes, pela
base HTTPS e com Bearer da sessão atual. O cliente não inclui `model_id`, nome de
modelo ou provedor no corpo. Histórico, nova conversa e exclusão mantêm seus
contratos anteriores; a remoção do seletor não altera dados financeiros.

Preservar mensagens e status de erro da API, incluindo 401 de sessão expirada e
403 de acesso bloqueado. Os erros do assistente usam `assistant_*`; manter leitura
de respostas antigas `gemini_quota_exceeded` e respeitar `retry_after_seconds`
positivo no bloqueio de cota.

A rotação do backend não significa uso ilimitado: limites globais e por usuário
continuam válidos e devem ser apresentados quando a API os informar. Não contornar
essas cotas, não fixar um tempo de espera e não reenviar automaticamente o POST de
chat em caso de erro. Falhas de rede, timeout ou JSON inválido permanecem visíveis,
sem repetir uma mensagem que o servidor pode já ter recebido.

## Validação

`AssistantAutomaticRoutingHttpTest`, `AssistantErrorsContractTest`,
`AssistantViewModelCommonTest` e `AssistantPresenterTest` cobrem o contrato de
envio sem `model_id`, tratamento de erros/cota, isolamento de sessão e continuidade
da conversa com dados sintéticos e fakes/MockEngine. A presença de testes não é
evidência de execução: registrar o
resultado do `harnessVerify` correspondente à alteração.

`AssistantAutomaticRoutingHttpTest` verifica POSTs de nova conversa e da mesma
conversa com somente `message`/`conversation_id`, sem consultar catálogo ou
preferências.

O harness padrão cobre JVM, APK Android, Lint e compilação KMP para os dois
targets Apple. Build/link Xcode e execução/comparação visual no iPhone continuam
validações separadas. Não iniciar dispositivo por iniciativa do agente.

Conferência manual pelo usuário nas duas plataformas:

- Abrir o Assistente e enviar uma mensagem diretamente, sem seletor ou espera
  por catálogo/preferência de modelo.
- Conferir conversa, rascunho e histórico ao sair e retornar à tela; testar nova
  conversa, exclusão e ditado.
- Conferir cota, indisponibilidade, timeout e sessão expirada sem perda indevida
  do rascunho nem reenvio automático.
- Testar logout/troca de conta, temas, tela estreita, fonte ampliada, leitor de
  tela e abertura/fechamento do teclado sem duplicar o espaço do compositor.
- Validar no app a rotação automática e o tratamento das
  preferências legadas, preservando a conversa e os limites reais do serviço.
