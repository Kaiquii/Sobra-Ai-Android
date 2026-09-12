# Orientações para agentes — SobraAi Android

## Comece aqui

Este repositório contém o aplicativo Android SobraAi, em Kotlin e Jetpack Compose,
com um módulo `:app`. O backend é externo: não presumir Go, Docker ou acesso ao
repositório do servidor a partir de informações de outra conversa.

Antes de editar:

1. Confira `git status --short` e preserve alterações existentes.
2. Leia [o mapa do projeto](docs/ai-project-map.md) e localize o fluxo afetado.
3. Leia [o fluxo de trabalho e validação](docs/ai-workflow.md).
4. Para mudanças visuais, leia integralmente [as regras de front-end](docs/frontend-rules.md).
5. Inspecione o código e os testes relevantes; a documentação é um mapa, não uma
   substituição da implementação. Atualize o mapa quando mudar a arquitetura.

Responda em português brasileiro. Execute o trabalho solicitado até a validação
adequada; se houver impedimento real, informe o que falta e a evidência. Não
declare testes, interface ou release aprovados sem ter feito essa validação.

## Identidade e organização

- `namespace` e `applicationId`: `br.com.sobraai.app`.
- Base dos fontes: `app/src/main/java/com/example/appfinanceiro`.
- O pacote Kotlin predominante é `com.example.appfinanceiro`; `R` e `BuildConfig`
  são gerados em `br.com.sobraai.app`. Não fazer renomeação global para igualá-los.
- `MainActivity.kt` concentra a navegação Compose e a entrada de sessão.
- `feature/` contém telas, componentes específicos e ViewModels.
- `core/data/` contém repositórios, contratos de fontes de dados e tratamento de erros.
- `core/network/` contém contratos Retrofit, DTOs e configuração HTTP.
- `core/designsystem/` contém tema e componentes reutilizáveis.
- Algumas pastas não coincidem com o `package`: conferir a declaração antes de importar.

## Regras de implementação

- Siga o padrão existente da funcionalidade. Para lógica nova, prefira ViewModels,
  funções testáveis e repositórios; não coloque chamadas HTTP na recomposição.
- Preserve os contratos de `FinanceDataSources.kt` e a possibilidade de injetar
  fakes nos ViewModels. Não introduza framework de DI ou dependências sem necessidade.
- Reutilize os componentes e tokens de tema existentes. Textos de interface são
  em português brasileiro, com acentos e arquivos UTF-8.
- Preserve nomes JSON, endpoints, parâmetros, tipos e valores de domínio da API.
  Verifique todos os consumidores ao alterar um DTO.
- Em regras financeiras, verifique parcelas, recorrência, adiantamentos, rateio,
  status de pagamento, arredondamento e filtros de mês/ano aplicáveis. Não mude
  unidade monetária, tipo numérico ou significado de saldo incidentalmente.
- Preserve propagação de cancelamento de coroutines e os estados de sessão/erro.
  Dados antigos não devem parecer pertencer a um filtro novo.
- Não exponha tokens, senhas, dados financeiros pessoais ou valores de configuração
  em logs, documentação, fixtures ou commits. Use dados sintéticos nos testes.
- Mantenha HTTPS, biometria, Play Integrity e regras de segurança de release.
  Não desative proteção para fazer um teste passar.
- Não edite saídas em `build/`, caches ou APK/AAB como se fossem fontes. Não altere
  versão, assinatura ou publique uma release como consequência de uma tarefa comum.

## Validação rápida

Execute os comandos a partir da raiz, com o Gradle Wrapper:

```powershell
.\gradlew.bat harnessUnit
.\gradlew.bat harnessVerify
```

O primeiro executa testes JVM; o segundo acrescenta Lint debug. Para dispositivo,
release, configuração do ambiente e critérios de conclusão, siga
[o fluxo de validação](docs/ai-workflow.md). O [harness de testes](docs/test-harness.md)
é uma ferramenta de apoio a estas instruções.

Mudanças apenas documentais precisam de revisão de conteúdo, caminhos e
`git diff --check`, não de recompilar todo o aplicativo.
