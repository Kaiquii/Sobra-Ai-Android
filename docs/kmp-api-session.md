# API e sessão compartilhadas

Esta etapa mantém o host e o front-end Android intactos. O backend é externo;
os testes usam MockEngine, persistência fake e dados sintéticos, sem login real.

## Organização e dependências

Na base `shared/src/commonMain/kotlin/com/example/appfinanceiro/`:

- `core/network/FinanceModels.kt` e `auth/AuthModels.kt`: DTOs serializáveis,
  packages originais, nomes JSON, `Double` e estados financeiros preservados.
- `SharedFinanceApi`/`KtorFinanceApi` e `auth/AuthApi`/`KtorAuthApi`: 29 endpoints
  JSON financeiros e cinco de autenticação. Incluem relatórios, assistente,
  operações financeiras, perfil e consulta de versão por plataforma.
- `ApiConfiguration` recebe a URL HTTPS pelo host. `ApiTransport` implementa
  headers, queries, erros e desserialização; não depende do `BuildConfig`.
- `core/data/SharedFinanceRepository`, `SharedAuthRepository` e
  `FinanceDataSources`: repositórios e contratos injetáveis nas duas plataformas.
- `SessionStore`, `PersistentSessionStore` e `SessionPersistence`: uma lógica
  de salvar identidade/avatar, biometria por conta e logout, com adaptadores nativos.

Ktor 3.2.3 usa OkHttp 4.12 no Android e Darwin no iOS. Ktor 3.6 foi descartado
porque seu OkHttp 5.5 exige compileSdk 37; os SDKs e bibliotecas visuais do app
foram preservados. kotlinx.serialization 1.11.0 usa o plugin Kotlin 2.4.21.

## Contratos de rede

Preservar verbos, barras finais, path, campos e queries dos contratos. Queries
nulas são omitidas; booleanos `false` são enviados. O repositório mantém
`Bearer`, `period_mode=effective`, rateio, parcelas, recorrência e adiantamento.
Valores nulos/ausentes inválidos não viram valores financeiros zerados.
Serializers específicos toleram somente texto de apresentação e rateios legados.

401 autenticado gera `SessionExpiredException`; 401 no login continua erro de
API. 403 contendo “Acesso revogado” em chamada autenticada emite
`SessionAccessEvents`, observado pelo Android existente. Outros erros preservam
`error`, `message` ou `detail`. Timeout e falha de conexão mantêm mensagens próprias;
cancelamento é propagado. Não há retry de aplicação nem replay automático de POST.
O engine OkHttp desabilita retry de conexão. Leituras seguem redirecionamentos
HTTPS; Ktor remove o Bearer se o host mudar e recusa downgrade para HTTP.
Escritas não são reenviadas automaticamente por redirecionamento.

O Android injeta uma fábrica de `ApiRequestException`, subclasse da exceção
comum `ApiResponseException`. Isso preserva os catches e smart casts locais da
tela de login, sem editar seu código. `userMessageOr` atende ambas.

`FinanceRepository` Android é uma fachada da implementação comum com o método de
upload de foto existente. Retrofit/Gson ficam somente para upload multipart e
download streaming de exportação; sua migração, seleção e armazenamento de
arquivos por plataforma pertencem à etapa de integrações. A exclusão de foto
por JSON já é comum e preserva sucesso HTTP vazio. Manter as regras R8 existentes.

## Sessão e compatibilidade Android

`shared/src/androidMain/.../core/data/SessionManager.kt` mantém package
`com.example.appfinanceiro.core.data`, construtor com `Context` e interface usada
pelas telas. A instância DataStore continua `user_session`, com as mesmas chaves:
`auth_token`, `user_name`, `user_email`, `user_role`, `user_avatar_url`,
`user_avatar_cache_version` e `biometric_enabled_emails`.

O adaptador lê os dados existentes e atualiza somente essas chaves. Logout remove
token/perfil/avatar, preservando biometria por e-mail e preferências desconhecidas.
Salvar avatar sem nova versão mantém o cache atual; avatar ausente/em branco
limpa URL e versão. Persistência e erros de leitura/escrita não são mascarados.

## Integração Apple

`shared/src/iosMain/` fornece engine Darwin e `IosSessionStore(service)`.
A identidade/token ficam em um único registro Keychain, com
`kSecAttrAccessibleWhenUnlockedThisDeviceOnly` e sincronização desabilitada;
a escolha de biometria por conta fica em `NSUserDefaults`. O host deve criar
uma instância de sessão e fornecer um identificador de serviço estável e exclusivo.
O store não executa biometria nem libera navegação. O host deve bloquear as telas
até o desbloqueio exigido antes de usar o token persistido; esse fluxo de
interface e LocalAuthentication ainda depende da etapa de UI/integrações.

`SecureSessionPersistence` serializa atualizações com mutex, carrega o registro
antes da primeira emissão e só publica a atualização após a persistência.
Falhas do Keychain e JSON corrompido são propagados, sem fabricar sessão válida.
Logout remove o registro seguro e mantém as preferências de biometria.

Exemplo mínimo de composição em Kotlin (o host completo está em `IosApp.kt`):

```kotlin
val httpClient = createPlatformHttpClient()
val transport = ApiTransport(httpClient, ApiConfiguration(apiBaseUrl))
val finance = SharedFinanceRepository(KtorFinanceApi(transport))
val auth = SharedAuthRepository(KtorAuthApi(transport))
val session: SessionStore = IosSessionStore(keychainService)
// Após login explícito, persistir response.token e response.user com saveToken.
// Encerrar httpClient.close() ao descartar a composição raiz que o possui.
```

O Android continua obtendo a URL da resolução Gradle existente em `BuildConfig`.
No host Apple, fornecer a configuração equivalente sem colocar credenciais de
servidor no código ou no binário.

## Proteção de autenticação

O Android injeta Play Integrity em `AuthRequestProtection`; paths e campos
`protection_provider`/`play_integrity_token` são preservados. Sem provedor,
solicitação de código e recuperação falham antes da chamada HTTP. Um provedor
Apple não é enviado sob o campo Play Integrity: depende de contrato e validação
App Attest no backend. Login, cadastro com código e redefinição têm transporte
comum. A interface já usa esse transporte para confirmar um código válido; o
envio no iPhone aguarda [o contrato Apple do backend](pendencias-backend-ios.md).

`FinanceFiles.kt` acrescenta uma fronteira Ktor para exportação e fotos no iOS:
mesmos endpoints, Bearer e parâmetros, upload JPEG multipart no campo `photo`,
limites de tamanho e nomes de arquivo sanitizados. Downloads de avatar em outro
domínio não recebem o token da API. A implementação Retrofit de arquivos do
Android permanece ativa. O erro de uma CDN pública não encerra a sessão da conta.

## Verificação

Na raiz, usar `harnessUnit` e `harnessVerify :app:assembleDebug`.
Os novos testes comuns verificam contratos HTTP, login/resumo/renda/despesa,
JSON legado, ausência de valor financeiro, erros, acesso revogado e cancelamento.
Testes de sessão usam persistência segura fake; o teste JVM Android lê um
DataStore temporário com as chaves antigas e confere logout e preservação.

Os fontes Kotlin/Native para iPhone (`compileKotlinIosArm64`) compilaram neste
Windows. Link do framework final, Keychain real, Darwin, biometria e navegação
exigem validação em Mac/Xcode e dispositivo. Consultar [shared/README.md](../shared/README.md).
Compilar fontes e executar testes JVM não comprova essas integrações em execução.

### Resultado em 19/10/2026

- Rodada final: `harnessVerify :app:assembleDebug` e compilação dos fontes/testes
  dos targets `iosArm64` e `iosSimulatorArm64`, sem erros. Tarefas Native já
  atualizadas foram reaproveitadas; os testes do simulador foram compilados nessa rodada.
- 44 testes JVM no app e 30 em `:shared`, sem falhas ou testes ignorados.
  O catálogo de contratos exercita 29 endpoints financeiros dentro desses testes.
- SHA-256: 68 fontes de front-end e 27 recursos/Manifest Android preservados.
- R8 e Lint release também concluíram após a migração dos modelos, antes do
  último ajuste de redirecionamento. A rodada final revalidou JVM/debug/Native;
  não é uma certificação de candidato release ou execução em dispositivo.
- Nenhuma chamada ao backend real, instalação, execução manual ou teste
  instrumentado/Native em dispositivo foi realizado. Keychain/Darwin reais,
  biometria, link de frameworks e interface Apple continuam pendentes.
