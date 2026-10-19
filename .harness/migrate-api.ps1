$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$utf8 = [Text.UTF8Encoding]::new($false)
$android = Join-Path $root 'app/src/main/java/com/example/appfinanceiro'
$common = Join-Path $root 'shared/src/commonMain/kotlin/com/example/appfinanceiro'
function Write-Source([string]$path, [string]$content) {
    [IO.Directory]::CreateDirectory((Split-Path $path)) | Out-Null
    [IO.File]::WriteAllText($path, $content, $utf8)
}
$financePath = Join-Path $android 'core/network/FinanceApi.kt'
$finance = [IO.File]::ReadAllText($financePath)
Write-Source (Join-Path $root '.harness/FinanceApi.before.kt') $finance
$modelStart = $finance.IndexOf('data class SummaryResponse')
$interfaceStart = $finance.IndexOf('interface FinanceApi')
$models = $finance.Substring($modelStart, $interfaceStart - $modelStart)
$models = [regex]::Replace($models, '(?m)^data class ', "@Serializable`ndata class ")
$models = $models.Replace('@SerializedName(', '@SerialName(')
$expenseStart = $models.IndexOf('data class Expense(')
$expenseEnd = $models.IndexOf('@Serializable', $expenseStart)
$expense = $models.Substring($expenseStart, $expenseEnd - $expenseStart)
foreach ($field in @('description', 'date', 'type')) {
    $expense = $expense.Replace("    val ${field}: String,", "    @Serializable(with = LegacyTextSerializer::class)`n    val ${field}: String = `"`",")
}
$expense = $expense.Replace('    val payment_splits: List<PaymentSplit>', "    @Serializable(with = LegacyPaymentSplitsSerializer::class)`n    val payment_splits: List<PaymentSplit>")
$models = $models.Substring(0, $expenseStart) + $expense + $models.Substring($expenseEnd)
$splitStart = $models.IndexOf('data class PaymentSplit(')
$splitEnd = $models.IndexOf('@Serializable', $splitStart)
$split = $models.Substring($splitStart, $splitEnd - $splitStart)
$split = $split.Replace('    val payment_source: String,', "    @Serializable(with = LegacyTextSerializer::class)`n    val payment_source: String = `"`",")
$models = $models.Substring(0, $splitStart) + $split + $models.Substring($splitEnd)
Write-Source (Join-Path $common 'core/network/FinanceModels.kt') ("package com.example.appfinanceiro.core.network`n`nimport kotlinx.serialization.SerialName`nimport kotlinx.serialization.Serializable`n`n" + $models)

$methodPattern = '(?s)@(?:retrofit2\.http\.)?(GET|POST|PATCH|DELETE)\("([^"]+)"\)\s+suspend fun (\w+)\((.*?)\): ([^\r\n]+)'
$methods = [regex]::Matches($finance.Substring($interfaceStart), $methodPattern)
$commonInterface = "package com.example.appfinanceiro.core.network`n`ninterface SharedFinanceApi {`n"
$implementation = "package com.example.appfinanceiro.core.network`n`nimport kotlinx.serialization.encodeToString`n`nclass KtorFinanceApi(private val transport: ApiTransport) : SharedFinanceApi {`n"
foreach ($method in $methods) {
    $verb = $method.Groups[1].Value
    $path = $method.Groups[2].Value
    $name = $method.Groups[3].Value
    $parameters = $method.Groups[4].Value
    $returnType = $method.Groups[5].Value.Trim()
    if ($name -in @('updateProfilePhoto', 'exportReport')) { continue }
    $clean = [regex]::Replace($parameters, '@(?:retrofit2\.http\.)?\w+\("[^"]+"\)\s*', '')
    $clean = [regex]::Replace($clean, '@(?:retrofit2\.http\.)?Body\s*', '')
    if ($name -eq 'deleteProfilePhoto') { $returnType = 'DefaultResponse' }
    $commonInterface += "    suspend fun ${name}(${clean}): $returnType`n`n"
    $overrideParameters = [regex]::Replace($clean, '\s*=\s*(?:"[^"]*"|null|false|true|\d+)', '')
    $implementation += "    override suspend fun ${name}(${overrideParameters}): $returnType =`n        transport.request(`n            method = `"$verb`", path = `""
    foreach ($pathParameter in [regex]::Matches($parameters, '@(?:retrofit2\.http\.)?Path\("([^"]+)"\)\s*(\w+)')) {
        $path = $path.Replace('{' + $pathParameter.Groups[1].Value + '}', '${' + $pathParameter.Groups[2].Value + '}')
    }
    $implementation += "$path`",`n"
    if ($parameters -match '@(?:retrofit2\.http\.)?Header\("Authorization"\)\s*(\w+)') {
        $implementation += '            authorization = ' + $Matches[1] + ",`n"
    } else { $implementation += "            authenticated = false,`n" }
    $queries = [regex]::Matches($parameters, '@(?:retrofit2\.http\.)?Query\("([^"]+)"\)\s*(\w+)')
    if ($queries.Count -gt 0) {
        $pairs = @($queries | ForEach-Object { '"' + $_.Groups[1].Value + '" to ' + $_.Groups[2].Value })
        $implementation += "            query = mapOf(" + ($pairs -join ', ') + "),`n"
    }
    if ($parameters -match '@(?:retrofit2\.http\.)?Body\s*(\w+)') {
        $implementation += '            body = FinanceJson.encodeToString(' + $Matches[1] + "),`n"
    }
    if ($name -eq 'deleteProfilePhoto') { $implementation += '            emptyResponse = "{\"message\":\"\"}",' + "`n" }
    $implementation += "        )`n`n"
}
Write-Source (Join-Path $common 'core/network/SharedFinanceApi.kt') ($commonInterface + "}`n")
Write-Source (Join-Path $common 'core/network/KtorFinanceApi.kt') ($implementation + "}`n")
# O transporte Retrofit remanescente é exclusivo dos arquivos nativos Android.
$nativeStart = $finance.IndexOf('    @Multipart', $interfaceStart)
$nativeEnd = $finance.IndexOf('    @DELETE("api/users/profile/photo")', $nativeStart)
$exportStart = $finance.IndexOf('    @Streaming', $interfaceStart)
$native = $finance.Substring(0, $modelStart).Replace("import com.google.gson.annotations.SerializedName", '')
$native += "interface FinanceApi {`n" + $finance.Substring($nativeStart, $nativeEnd - $nativeStart) + $finance.Substring($exportStart)
Write-Source $financePath $native

$authPath = Join-Path $android 'core/network/auth/AuthApi.kt'
$auth = [IO.File]::ReadAllText($authPath)
Write-Source (Join-Path $root '.harness/AuthApi.before.kt') $auth
$authStart = $auth.IndexOf('data class LoginRequest')
$authEnd = $auth.IndexOf('interface AuthApi')
$authModels = [regex]::Replace($auth.Substring($authStart, $authEnd - $authStart), '(?m)^data class ', "@Serializable`ndata class ")
Write-Source (Join-Path $common 'core/network/auth/AuthModels.kt') ("package com.example.appfinanceiro.core.network.auth`n`nimport kotlinx.serialization.Serializable`n`n" + $authModels)
$authInterfaceEnd = $auth.IndexOf('object RetrofitClient')
$authInterface = $auth.Substring($authEnd, $authInterfaceEnd - $authEnd)
$authInterface = [regex]::Replace($authInterface, '(?m)^    @POST\([^\r\n]+\r?\n', '')
$authInterface = $authInterface.Replace('@Body ', '')
Write-Source (Join-Path $common 'core/network/auth/AuthApi.kt') ("package com.example.appfinanceiro.core.network.auth`n`n" + $authInterface)
$retrofit = $auth.Substring($authInterfaceEnd)
$authApiStart = $retrofit.IndexOf('    val authApi:')
$authApiEnd = $retrofit.IndexOf('    val financeApi:')
$retrofit = $retrofit.Remove($authApiStart, $authApiEnd - $authApiStart)
# Sem log de URL/dados financeiros; este cliente só atende foto e exportação.
$interceptorStart = $retrofit.IndexOf('            .addInterceptor')
$interceptorEnd = $retrofit.IndexOf('            .build()', $interceptorStart)
$retrofit = $retrofit.Remove($interceptorStart, $interceptorEnd - $interceptorStart)
Write-Source $authPath ("package com.example.appfinanceiro.core.network.auth`n`nimport br.com.sobraai.app.BuildConfig`nimport com.example.appfinanceiro.core.network.FinanceApi`nimport com.example.appfinanceiro.core.network.createFinanceGson`nimport okhttp3.OkHttpClient`nimport retrofit2.Retrofit`nimport retrofit2.converter.gson.GsonConverterFactory`nimport java.util.concurrent.TimeUnit`n`n" + $retrofit)

foreach ($relative in @('core/data/FinanceDataSources.kt', 'core/data/SessionExpiredException.kt', 'core/network/SessionAccessEvents.kt')) {
    Write-Source (Join-Path $common $relative) ([IO.File]::ReadAllText((Join-Path $android $relative)))
    Remove-Item -LiteralPath (Join-Path $android $relative)
}
$repoPath = Join-Path $android 'core/data/FinanceRepository.kt'
$repo = [IO.File]::ReadAllText($repoPath)
Write-Source (Join-Path $root '.harness/FinanceRepository.before.kt') $repo
$repo = $repo.Replace('import com.example.appfinanceiro.core.network.auth.RetrofitClient', '').Replace('import okhttp3.MultipartBody', '')
$repo = $repo.Replace('class FinanceRepository :', 'open class SharedFinanceRepository(private val api: SharedFinanceApi) :')
$repo = $repo.Replace('RetrofitClient.financeApi', 'api').Replace('executeApiRequest(block = block)', 'block()')
$uploadStart = $repo.IndexOf('    suspend fun updateProfilePhoto(')
$deleteStart = $repo.IndexOf('    suspend fun deleteProfilePhoto(', $uploadStart)
$repo = $repo.Substring(0, $uploadStart) + @'
    suspend fun deleteProfilePhoto(token: String): DefaultResponse = api.deleteProfilePhoto(bearer(token))
}
'@
Write-Source (Join-Path $common 'core/data/SharedFinanceRepository.kt') $repo
Write-Source $repoPath @'
package com.example.appfinanceiro.core.data

import com.example.appfinanceiro.core.network.SharedFinanceApi
import com.example.appfinanceiro.core.network.ProfilePhotoResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import com.example.appfinanceiro.core.network.AndroidApiClient
import okhttp3.MultipartBody

class FinanceRepository(api: SharedFinanceApi = AndroidApiClient.financeApi) : SharedFinanceRepository(api) {
    suspend fun updateProfilePhoto(token: String, photo: MultipartBody.Part): ProfilePhotoResponse =
        executeApiRequest { RetrofitClient.financeApi.updateProfilePhoto("Bearer $token", photo) }
}
'@
Write-Output ('Modelos e contratos compartilhados; endpoints JSON: ' + ($methods.Count - 2))
