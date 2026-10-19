# Plano aprovado — Fundação KMP

Estado: Gate 2 aprovado; implementação em andamento.
Base: `0183c31`, branch de origem `main`.
Aprovação: “Aprovo o plano, pode começar a implementação mantendo o front-end do Android exatamente como está.”

## Contrato

Preservar integralmente telas, componentes visuais, tema, recursos, textos e
navegação Android. Regras extraídas mantêm pacotes, assinaturas e resultados.
Objetivo final: interface Compose Multiplatform compartilhada; nesta entrega,
biblioteca comum sem migração de UI, rede ou sessão.

## Matriz candidata

Kotlin/KMP/compilador Compose 2.4.21; AGP 9.2.0; Wrapper 9.4.1; daemon JDK 21;
bytecode Android 11. Preservar bibliotecas visuais. Biblioteca Android KMP via
`com.android.kotlin.multiplatform.library`; Apple `iosArm64` e `iosSimulatorArm64`.
Compatibilidade publicada não substitui resolução e build reais.

## Time e instruções

Gael — Kotlin/KMP — `gpt-5.6-sol/high`, substituição explicitamente aprovada ao
aprovar o plano, pois `gpt-5.6-terra` não está disponível para subagentes.
Sessão principal: coordenação, integração e builds.
Revisão independente será detalhada no Gate 3.

Instruções por tarefa: `AGENTS.md`, `docs/ai-project-map.md`,
`docs/ai-workflow.md`, `docs/plano-migracao-kmp.md` e
`C:/Users/kaiqu/.codex/skills/desenvolver/references/roles/desenvolvedor.md`.
Não há skill Kotlin/KMP específica instalada.

## Sprint 1 — Gael, em ordem

### 1.1 Referência e compatibilidade

Sessão registra `harnessVerify` e `:app:assembleDebug` antes da primeira alteração.
Gael atualiza `gradle/libs.versions.toml`, plugins em `build.gradle.kts` e ajustes
estritamente necessários em `app/build.gradle.kts`. Preservar identidade, SDK,
versão, API, assinatura, bibliotecas visuais e proteções.
Aceite: resolução e build Android aprovados; separar falhas anteriores.

### 1.2 Shared e regras

Criar `shared/build.gradle.kts`, `shared/src/commonMain/`, `shared/src/commonTest/`.
Integrar em `settings.gradle.kts`, dependência de `:app` e `.gitignore`.
Mover `MonthNavigation.kt` e `ExpenseFormValidator.kt` para código comum, com os
mesmos packages/assinaturas. Substituir floorDiv/floorMod JVM por equivalentes
Kotlin. Seis consumidores de meses e dois de formulário ficam sem edição.
Portar testes para `kotlin.test`; manter no Android testes de rateio existentes.
Configurar testes locais Android e framework estático Apple `SobraAiShared`.
Aceite: um código das regras; virada de ano, deslocamentos negativos, mês
inválido e limites do formulário cobertos; validação Native pendente em Windows.
Sessão compila `:app:assembleDebug` após retorno; dev roda só unitários dirigidos.

## Sprint 2 — Gael; aguarda build da Sprint 1

### 2.1 Harness

Atualizar tarefas raiz e `scripts/test-harness.ps1`: testes reais de `:shared`
incluídos em `harnessUnit`/`harnessVerify`, relatórios identificados, modos de
dispositivo preservados. Não presumir aprovação Apple a partir da execução JVM.

### 2.2 Documentação

Atualizar `docs/ai-project-map.md`, `docs/ai-workflow.md`, `docs/test-harness.md`,
`docs/plano-migracao-kmp.md` e criar `shared/README.md` com versões, módulos,
comandos reais, integração futura com Xcode e pendências Apple. Preservar revisão
anterior no plano de migração. Aceite: conteúdo reflete código, links válidos e
`git diff --check` aprovado. Sessão compila após retorno.

## Organização e validação

Trabalho: `.harness/work/desenvolver/2026-10-19-fundacao-kmp/`.
Branch: `main`, conforme correção explícita do usuário durante a execução:
“não era pra criar branch nova, tudo na Main como sempre”. Preservar modificações existentes.
Sem commit, push, release ou publicação. Builds serializados pela sessão;
unitários dirigidos somente quando a sessão não estiver usando Gradle.
Nenhum emulador iniciado, app instalado/aberto ou instrumentado executado.
Gate 3 detalhará regressão final, Lint, testes, build e revisão independente.
Mac/Xcode e iPhone não disponíveis neste Windows: nenhuma validação Apple PASS.

## Revisão do plano

Duas passadas; última sem bloqueantes para preparação Windows. Duas sprints,
quatro tarefas, um desenvolvedor. Sem banca adicional na preparação.
Alteração anterior em `docs/plano-migracao-kmp.md` é superfície preservada.
