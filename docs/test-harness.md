# Harness de testes

O harness oferece uma entrada unica para validar o aplicativo localmente e em
automacoes. Ele usa o Gradle Wrapper do repositorio e preserva os relatorios HTML
padrao do Android Gradle Plugin.

## Uso no Windows

Execute a partir da raiz do projeto:

```powershell
.\scripts\test-harness.ps1
```

O modo padrao, `unit`, executa apenas os testes unitarios e nao requer emulador.

| Modo | Validacoes | Precisa de dispositivo |
| --- | --- | --- |
| `unit` | Testes JVM de `:app`, `:shared` e `:sharedUi` | Nao |
| `verify` | Testes JVM dos três módulos e Android Lint debug | Nao |
| `device` | Testes instrumentados | Sim |
| `all` | Testes unitarios, Lint e instrumentados | Sim |

Exemplos:

```powershell
.\scripts\test-harness.ps1 -Mode verify
.\scripts\test-harness.ps1 -Mode device
.\scripts\test-harness.ps1 -Mode all -NoDaemon
```

Para usar uma URL diferente sem alterar `local.properties`:

```powershell
.\scripts\test-harness.ps1 -Mode unit -ApiBaseUrl "https://api.exemplo.com"
```

A URL também pode ser fornecida pela variavel de ambiente `API_BASE_URL` ou pela
propriedade Gradle `-PAPI_BASE_URL`. O valor deve usar HTTPS.

## Uso direto pelo Gradle

Os mesmos grupos podem ser chamados sem o script:

```powershell
.\gradlew.bat harnessUnit
.\gradlew.bat harnessVerify
.\gradlew.bat harnessDevice
.\gradlew.bat harnessAll
```

O script e recomendado para os modos que usam dispositivo porque confirma antes
da execucao se o ADB e um dispositivo ou emulador estao disponiveis.

## Relatorios

Depois da execucao, os relatorios ficam em:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `shared/build/reports/tests/testAndroidHostTest/index.html`
- `app/build/reports/lint-results-debug.html`
- `app/build/reports/androidTests/connected/debug/index.html`

Uma falha em qualquer etapa encerra o harness com codigo diferente de zero, o que
permite utiliza-lo como verificacao em CI.

## Kotlin Multiplatform

`harnessUnit` depende de `:app:testDebugUnitTest`, `:shared:testAndroidHostTest` e
`:sharedUi:testAndroidHostTest`. O relatório da interface comum fica em
`sharedUi/build/reports/tests/testAndroidHostTest/index.html`.
`harnessVerify` inclui os dois e `:app:lintDebug`. O script mostra separadamente
o relatório das regras comuns executadas na JVM. Os modos `device`/`all`
preservam o comportamento Android e exigem solicitação explícita para execução.

Os targets Apple não são validados por esse harness. Para compilar frameworks
e executar testes Native em Mac/Xcode, seguir [shared/README.md](../shared/README.md).
O aviso de teste Native desabilitado no Windows não deve ser suprimido nem
interpretado como teste iOS aprovado.
