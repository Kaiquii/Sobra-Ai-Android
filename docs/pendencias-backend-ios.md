# Pendências do backend para iOS

Estado em 19/10/2026: o responsável confirmou que **a API ainda não tem contrato
Apple** e autorizou deixar esta integração pendente. Não há acesso ao repositório
do servidor neste projeto. Os itens abaixo são requisitos a definir com o backend,
não endpoints novos já disponíveis.

## Envio de códigos de cadastro e recuperação

Os contratos atuais de `POST /api/auth/request-register-code` e
`POST /api/auth/forgot-password` exigem `protection_provider=play_integrity` e
`play_integrity_token`. Esses campos continuam intactos para o Android.
Um iPhone não produz Play Integrity; não enviar token vazio, fictício ou um token
Apple sob o nome de um provedor Android.

O backend precisa definir e implementar um protocolo Apple. A referência para
provar a integridade da instância do app é
[App Attest / DeviceCheck](https://developer.apple.com/documentation/DeviceCheck).
Face ID/Touch ID autentica localmente a pessoa e não substitui essa prova.

Requisitos do contrato a acordar:

1. Definir rotas, métodos, JSON e versão do protocolo para obter um desafio,
   registrar uma chave atestada e autorizar cada pedido de código.
2. Desafios aleatórios, curtos, de uso único, com validade e vínculo ao propósito
   (cadastro ou recuperação), e-mail normalizado e conteúdo da requisição.
3. Verificar no servidor a atestação Apple, identidade esperada do aplicativo,
   ambiente de desenvolvimento/produção e, nas requisições seguintes, a assinatura
   da asserção, o desafio e a proteção contra repetição. Guardar o estado necessário
   por chave; não confiar apenas em uma indicação enviada pelo cliente.
4. Manter limites por e-mail, origem e dispositivo, expiração/uso único do OTP e
   respostas que não revelem indevidamente a existência de uma conta.
5. Definir erros estáveis (desafio expirado, prova inválida, limite, chave revogada),
   política de nova tentativa e `Retry-After`. Não repetir automaticamente uma
   operação que possa ter enviado e-mail.
6. Definir comportamento seguro para aparelho sem suporte, simulador, reinstalação
   e troca de chave. Qualquer alternativa precisa ser validada pelo servidor;
   não liberar pedidos sem proteção em produção.
7. Fornecer exemplos sintéticos de requisições/respostas e ambiente de teste,
   sem credenciais ou provas reais no repositório.

Após a definição, criar o adaptador nativo e implementar `AppleCodeDelivery`,
injetando-o em `IosApp`. Hoje `PendingAppleCodeDelivery.available=false`: a tela
explica a indisponibilidade e não chama os endpoints protegidos. O usuário pode
informar um código válido obtido por outro canal autorizado; o servidor continua
validando esse código nos endpoints existentes de cadastro/redefinição. Não há
geração local ou bypass de OTP.

`POST /api/auth/register` (`name`, `email`, `password`, `code`) e
`POST /api/auth/reset-password` (`email`, `code`, `new_password`) já estão ligados
aos formulários comuns. A validação integrada do envio e do ciclo completo fica
pendente do contrato acima e de testes no ambiente Apple.

## Consulta de versão iOS

O contrato atual de versão contém `play_store_url`; confirmar no backend se há
suporte a `platform=ios`, quais versões serão publicadas e qual campo/endereço
da App Store será usado. O bloqueio de atualização do Android permanece no host
Android. Não inventar versão mínima, link de loja ou equivalência entre os dois
canais. Esta pendência não bloqueia login e uso das telas com conta existente.

## Critério de encerramento

- Contrato Apple revisado e implantado pelo backend.
- Cliente nativo implementado com as capacidades e identificadores Apple corretos.
- Provas válidas, inválidas, expiradas e repetidas testadas, incluindo limites e
  cancelamento; regressão Android aprovada.
- Cadastro e recuperação completos executados em iPhone com envio real autorizado.

Build no Xcode, testes de dispositivo e serviços Apple continuam sendo validações
distintas. Compilar os fontes Kotlin no Windows não encerra esta pendência.
