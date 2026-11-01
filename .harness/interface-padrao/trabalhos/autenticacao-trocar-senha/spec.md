# Recuperar senha / Nova senha — mock v4

## Aceite e implementação

31/10/2026, usuário: “Gostei das melhorias, pode fazer”. Aceite explícito da
v4 de Recuperar senha/Nova senha, somado ao aceite anterior de login, cadastro
e confirmação de e-mail do conjunto de autenticação. Implementar o conjunto
compartilhado Android/iOS, sem alterar versão, endpoints ou regras de sessão.
Referências adicionais externas: `stitch-auth/1.png`, `2.png`, `3.png` e `6.png`.
Não implementar ajuda, contador de expiração ou garantias fictícias dos mocks.

Lacuna de composição: os três consumidores repetiam a estrutura de página,
mas não havia shell comum de autenticação com marca, retorno, área segura e
footer responsivo. Compor `AuthPage(onBack?, enabled, footer?, content)` no
mesmo pacote compartilhado de AuthComponents, sem navegação/store embutidos.
Conteúdo e footer rolam quando teclado/fonte reduzem a altura; botões de envio
continuam no fluxo abaixo dos campos, especialmente Enviar código.

Extensões compatíveis: AuthHeader aceita ícone opcional e alinhamento de texto
(default central para preservar GateScreen); AuthTextField/AuthPasswordField
aceitam ícone inicial opcional e ações/opções do teclado. Reutilizar campos,
botões e mensagens existentes, sem kit novo. Alturas mínimas, não fixas, para
preservar fonte ampliada; cores do tema e tokens existentes claro/escuro.

Lacuna na apresentação do código do cadastro: `AuthCodeField(label, value,
onValueChange, enabled, keyboardOptions, keyboardActions)` compõe BasicTextField
único com seis células visuais, permitindo colar/deletar como um único valor.
As células não geram seis focos nem repetem as semânticas do texto editável.
Código na recuperação permanece um campo simples, conforme mock aprovado.

Verificação: harnessVerify, revisão independente do diff e limites manuais de
interface. O usuário prepara/testa aparelhos; não iniciar app/emulador para
conseguir captura. Sem execução real, paridade visual e teclado continuam
pendentes, mesmo com build aprovado.

Correção do usuário em 31/10/2026: enviou as duas telas atuais, Recuperar Senha
e Nova Senha, e indicou que a representação anterior não correspondia bem ao
aplicativo. A v2 não foi aprovada pelo usuário. A proposta atual apresenta as DUAS
etapas: (1) e-mail e Enviar código; (2) e-mail reaproveitado, código, nova senha,
confirmação e Atualizar senha. Preservar títulos e transição existentes, usando
a linguagem visual aprovada no login/cadastro; não trocar o fluxo por uma única
tela genérica Trocar senha. Capturas fornecidas são apenas referências locais;
não transmitir dados pessoais ao Stitch nem reproduzi-los nos mocks/registros.

Artefatos atuais externos, na pasta de visualização da sessão `stitch-auth/`:
`recuperar-senha-v4.png` e `nova-senha-v4.png`. Telas Stitch respectivamente
`921aeb0c0c0a436cbc77722832592de8` e `c2da323aa2cd4a54a29185ff3fb67398`,
ambas MOBILE 780 × 1768. V4 corrige o grande vazio entre e-mail e botão da v3:
Enviar código fica logo após o campo, como na tela real, e ambas reservam área
de status no topo. Revisão e aceite posteriores estão registrados acima e no parecer.

Pedido de 31/10/2026: após aprovar a proposta de autenticação, visualizar Trocar
senha. Escopo atual: proposta do Stitch, sem alterar código de produção.

Referência aprovada: login escuro do projeto Stitch 6150502095301066848,
tela 9ddc2627928a43ec8140259af7bd3fc5. Prévia externa preservada em
`C:/Users/kaiqu/.codex/visualizations/2026/09/12/01a09731-9477-7850-acb4-eea0c7a8b2ed/stitch-auth/1.png`.

Interpretação inicial explicitada ao usuário: etapa de redefinição pelo fluxo
atual de recuperação. O usuário corrigiu a apresentação parcial; v3 cobre as
duas etapas. Não foi identificado fluxo de senha atual/troca autenticada na
busca dos fontes comuns; não propor um contrato novo para isso.

| Bloco | Fonte/reuso previsto | Aparência/diferença proposta |
| --- | --- | --- |
| Página e retorno | EsqueciSenhaScreen, coluna rolável/áreas seguras/teclado | Mesma página e fluxo; hierarquia alinhada ao login aprovado |
| Cabeçalho | AuthHeader e login aprovado | Marca compacta, títulos Recuperar senha / Nova senha à esquerda, sem ajuda fictícia |
| E-mail e código | AuthTextField, consumidor EsqueciSenhaScreen | Campos #202934, raio 12 dp, rótulos persistentes; código de seis dígitos |
| Senhas | AuthPasswordField, mesmo consumidor | Nova senha e confirmação, olhos existentes |
| Ação e erro | AuthPrimaryButton/AuthErrorMessage | Azul #1877F2, texto branco, altura aproximada 54 dp; erro só quando aplicável |

Viewport de referência: celular aproximadamente 390 × 844, tema escuro.
Critérios: um campo na primeira etapa e quatro na segunda, nenhuma senha atual nova, sem dados reais,
sem contador de expiração ou garantias de segurança inventadas. Sem ilustração
grande nem card externo. Representação estática não comprova teclado, foco ou
acessibilidade executados; implementação e validação manual são etapas futuras.

Artefato v2: prévia externa `stitch-auth/trocar-senha-v2.png` na mesma pasta de
visualização. Tela Stitch `9b88351d58314381902563e8c32bcd3b`, MOBILE, imagem
780 × 1768 (aproximadamente 390 × 884 lógicos). A v1 gerou quadro desktop
2560 × 2048; foi substituída pela v2 mobile antes da apresentação final.
Parecer independente da fase mock em parecer.md. A v2 não recebeu aceite humano;
a implementação usa o aceite posterior da v4, registrado no início deste documento.

## Ajuste solicitado após teste em 31/10/2026

O usuário pediu retirar o ícone extra de Recuperar senha por deslocar o título e
os campos para baixo. Cabeçalhos de login, cadastro, confirmação de e-mail e
recuperação usam AuthHeader sem ícone no corpo; a marca compacta SobraAi no topo
e os ícones dos estados de proteção (GateScreen) permanecem.
