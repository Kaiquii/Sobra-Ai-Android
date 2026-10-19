# Entendimento aprovado — Fundação KMP

Estado: Gate 1 aprovado; Gate 2 em preparação.
Base: HEAD `0183c31`, branch `main`, consultada em 19/10/2026 às 09:06
(America/Sao_Paulo). Revisão do entendimento: uma passada, sem pendências de escopo.

## Pedido original

> Pode começar pela primeira etapa do plano, ajustando a compatibilidade e preparando a base KMP sem quebrar o Android.
>
> Não é pra mudar nada no Front-end do Android, é pra deixar o IOS 100%, basicamente o mesmo código tanto pra Android e IOS

## Aprovação recebida

> Aprovo, pode seguir com o plano de implementação, mantendo o front-end do Android exatamente como está.

Essa aprovação libera a preparação do plano de implementação (Gate 2).
Não houve aprovação do Gate 2 nem início da implementação neste registro.

## Objetivo entendido

Iniciar a fundação para Android e iOS compartilharem código. O objetivo final é
paridade funcional completa no iPhone, incluindo interface compartilhada com
Compose Multiplatform e adaptadores para serviços nativos.

Esta entrega inicial prepara a compatibilidade e o módulo compartilhado. A
conclusão do iOS completo exige as próximas etapas do plano de migração.

## Situação comprovada

As referências abaixo partem da raiz do repositório. A configuração foi conferida
no working tree e, para catálogo e módulos, também com `git show HEAD:<arquivo>`.

| Evidência | Consequência |
| --- | --- |
| `settings.gradle.kts:26` inclui somente `:app` | Ainda não existe módulo KMP. |
| `gradle/libs.versions.toml:2` define AGP 9.2.0 e a linha 9 define Kotlin 2.2.10 | A matriz precisa ser ajustada antes da adoção de KMP. |
| `gradle/wrapper/gradle-wrapper.properties:5` usa Gradle 9.4.1 | A seleção de Kotlin precisa considerar esse Wrapper. |
| `app/src/main/java/com/example/appfinanceiro/core/date/MonthNavigation.kt:13` usa operações JVM | Há regras pequenas que podem iniciar o compartilhamento, preservando resultados. |

A combinação atual está fora da faixa publicada para Kotlin/KMP 2.2.10.
Isso não comprova falha do Android atual.
Referência: https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html

## Decisões já fornecidas

- Front-end Android preservado integralmente.
- Paridade completa do iOS como objetivo final.
- Máximo de código compartilhado entre as plataformas.
- Começar pela compatibilidade e fundação.

## Rumo desta entrega

1. Registrar a validação Android de referência.
2. Ajustar Kotlin e plugins para uma combinação compatível.
3. Criar `:shared`, com targets Android e iOS.
4. Mover regras pequenas e testes para código comum, mantendo os consumidores
   Android e os resultados existentes.
5. Preparar a geração do framework Apple e documentar a integração.
6. Atualizar harness e mapa do projeto; validar Android com testes JVM, Lint e build.

## Onde haverá alterações

Configurações Gradle, novo módulo compartilhado, regras selecionadas, testes e
documentação. Layouts, componentes visuais, tema, textos e navegação Android
permanecem preservados.

## Limites e validação

A migração de telas, rede, sessão e serviços nativos completos pertence às etapas
seguintes. Compilação e execução Apple dependem de Mac/Xcode; não serão declaradas
aprovadas neste Windows. Nenhum código foi alterado na etapa de entendimento.

## Superfície anterior preservada

`docs/plano-migracao-kmp.md` já estava modificado pela revisão documental anterior
desta conversa. Preservar essas alterações ao acrescentar o estado da implementação.
