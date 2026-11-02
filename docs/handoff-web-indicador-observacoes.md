# Handoff — restaurar indicador de observações nos cards

Pedido aprovado em 01/11/2026. Destino: Sobra-Ai-Web, chat
“Ajustar avatar_url no front-end”. A versão web mobile deve acompanhar o Android;
desktop pode acomodar melhor a largura sem mudar o significado do indicador.

## O que restaurar

- Nos cards de despesas do Início e da lista de Despesas, mostrar um pequeno
  ícone de documento quando `notes` contiver texto. Ausência, null, string vazia
  ou apenas espaços não mostram o ícone. Não depender de categoria, tipo,
  pagamento, adiantamento ou filtro para determinar sua presença.
- Posicionar na mesma linha de tipo/data/Adiantada. Preservar o card compacto,
  a altura e a posição das ações existentes; não criar outra linha de botões,
  carta de observações ou ícone grande de recibo.
- Referência Android: Material Description de 12 dp, padding de 4 dp (20 dp no
  total), fundo azul primário com opacidade de 10%, raio de 6 dp e distância de
  6 dp dos metadados. Adaptar às unidades CSS e aos tokens claro/escuro da web.
- É informativo, sem ação de clique. Nome acessível “Tem observações”; o texto
  completo continua no modal de detalhes, acessível pelo botão de visualizar.
  Não expor o conteúdo da observação em tooltip, logs ou rótulo acessível.
- Em pouca largura/fonte ampliada, reservar espaço para o indicador e manter
  metadados consultáveis sem deslocar as ações. Android usa rolagem horizontal
  somente na área de metadados quando ela não cabe.

## Fontes Android/KMP

- `designSystem/src/commonMain/kotlin/com/example/appfinanceiro/core/designsystem/components/ExpenseCard.kt`:
  parâmetro opcional `notes`, estilos Compact/Detailed e ExpenseNoteIndicatorIcon.
- `designSystem/.../feature/home/components/DespesasSection.kt`: ExpenseItem
  encaminha notes para o card Compact, recebido de HomeScreen.
- `sharedUi/.../feature/despesas/DespesasScreen.kt`: passa expense.notes ao Detailed.

Não é necessária alteração no backend: usar o campo notes já recebido, preservando
o contrato e o conteúdo integral. Pesquisar os consumidores web equivalentes e
reutilizar o componente de card/ícones existente; não corrigir só uma lista.

## Conferência

Testar com texto, null, vazio e espaços; despesa única/parcelada/fixa, paga e
pendente, adiantada, após criar/editar/remover as observações, e listas filtradas.
Conferir ambos os temas, mobile estreito e fonte ampliada. Somente o indicador
deve aparecer/desaparecer; conteúdo completo permanece nos detalhes.

Este documento descreve o handoff, não comprova implementação ou testes da web.
