[CmdletBinding()]
param(
    [ValidateSet("unit", "kmp", "verify", "device", "all")]
    [string] $Mode = "verify",

    [string] $ApiBaseUrl,

    [switch] $NoDaemon
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$localProperties = Join-Path $projectRoot "local.properties"

function Stop-Harness([string] $Message) {
    Write-Error "[harness] $Message"
    exit 1
}

function Get-AndroidSdkPath {
    if ($env:ANDROID_HOME) {
        return $env:ANDROID_HOME
    }

    if ($env:ANDROID_SDK_ROOT) {
        return $env:ANDROID_SDK_ROOT
    }

    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -match '^sdk\.dir=' } |
            Select-Object -First 1

        if ($sdkLine) {
            return ($sdkLine -replace '^sdk\.dir=', '') -replace '\\\\', '\'
        }
    }

    return $null
}

function Assert-AndroidDevice {
    $sdkPath = Get-AndroidSdkPath
    $adb = if ($sdkPath) { Join-Path $sdkPath "platform-tools\adb.exe" } else { $null }

    if (-not $adb -or -not (Test-Path -LiteralPath $adb)) {
        $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
        if ($adbCommand) {
            $adb = $adbCommand.Source
        }
    }

    if (-not $adb -or -not (Test-Path -LiteralPath $adb)) {
        Stop-Harness "ADB nao encontrado. Configure sdk.dir em local.properties ou ANDROID_HOME."
    }

    $devices = & $adb devices 2>&1
    if ($LASTEXITCODE -ne 0) {
        Stop-Harness "Nao foi possivel consultar o ADB: $($devices -join ' ')"
    }

    $readyDevices = @($devices | Where-Object { $_ -match "\sdevice$" })
    if ($readyDevices.Count -eq 0) {
        Stop-Harness "Nenhum dispositivo ou emulador Android pronto foi encontrado."
    }

    Write-Host "[harness] Dispositivo: $($readyDevices[0])"
}

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    Stop-Harness "gradlew.bat nao foi encontrado em $projectRoot."
}

$hasConfiguredApi = $ApiBaseUrl -or $env:API_BASE_URL -or
    ((Test-Path -LiteralPath $localProperties) -and
        (Select-String -LiteralPath $localProperties -Pattern '^\s*API_BASE_URL\s*=\s*https://' -Quiet))

if (-not $hasConfiguredApi) {
    Stop-Harness "Configure uma URL HTTPS via -ApiBaseUrl, API_BASE_URL ou local.properties."
}

if ($ApiBaseUrl -and -not $ApiBaseUrl.StartsWith("https://")) {
    Stop-Harness "-ApiBaseUrl precisa usar HTTPS."
}

if ($Mode -in @("device", "all")) {
    Assert-AndroidDevice
}

$taskByMode = @{
    unit = "harnessUnit"
    kmp = "harnessKmp"
    verify = "harnessVerify"
    device = "harnessDevice"
    all = "harnessAll"
}

$gradleArguments = @($taskByMode[$Mode], "--stacktrace")
if ($NoDaemon) {
    $gradleArguments += "--no-daemon"
}
if ($ApiBaseUrl) {
    $normalizedApiUrl = $ApiBaseUrl.Trim().TrimEnd('/')
    $gradleArguments += "-PAPI_BASE_URL=$normalizedApiUrl"
}

Write-Host "[harness] SobraAi KMP: preservar Android e iPhone em toda alteracao."
Write-Host "[harness] Revisao transversal obrigatoria: conferir consumidores e fluxos semelhantes; nao concluir com o mesmo defeito conhecido em outra tela."
Write-Host "[harness] Executando modo '$Mode'..."
if ($Mode -eq "unit") {
    Write-Host "[harness] Modo parcial: somente JVM. Use verify para compilar tambem os fontes iOS."
}
if ($Mode -eq "device") {
    Write-Host "[harness] Modo parcial: instrumentados Android. Nao substitui verify nem validacao Apple."
}
Push-Location $projectRoot
try {
    & $gradleWrapper @gradleArguments
    if ($LASTEXITCODE -ne 0) {
        Stop-Harness "O Gradle encerrou com codigo $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

$unitReport = Join-Path $projectRoot "app\build\reports\tests\testDebugUnitTest\index.html"
$sharedUnitReport = Join-Path $projectRoot "shared\build\reports\tests\testAndroidHostTest\index.html"
$sharedUiReport = Join-Path $projectRoot "sharedUi\build\reports\tests\testAndroidHostTest\index.html"
$lintReport = Join-Path $projectRoot "app\build\reports\lint-results-debug.html"
$deviceReport = Join-Path $projectRoot "app\build\reports\androidTests\connected\debug\index.html"
$apkDirectory = Join-Path $projectRoot "app\build\outputs\apk\debug"

Write-Host "[harness] OK: verificacoes do modo '$Mode' concluidas."
if ($Mode -in @("kmp", "verify", "all")) {
    Write-Host "[harness] KMP: fontes Android e fontes/testes KLIB iosArm64 + iosSimulatorArm64 de shared/sharedUi compilados."
}
Write-Host "[harness] Pendente neste host Windows: link Apple, build Swift/Xcode e execucao no Mac/iPhone."
Write-Host "[harness] Guia Apple: README.md e docs/test-harness.md."
if ($Mode -in @("unit", "verify", "all") -and (Test-Path -LiteralPath $unitReport)) {
    Write-Host "[harness] Testes unitarios: $unitReport"
}
if ($Mode -in @("unit", "verify", "all") -and (Test-Path -LiteralPath $sharedUnitReport)) {
    Write-Host "[harness] Testes compartilhados (Android/JVM): $sharedUnitReport"
}
if ($Mode -in @("verify", "all") -and (Test-Path -LiteralPath $lintReport)) {
    Write-Host "[harness] Lint: $lintReport"
}
if ($Mode -in @("verify", "all") -and (Test-Path -LiteralPath $apkDirectory)) {
    Write-Host "[harness] APK Android debug: $apkDirectory"
}
if ($Mode -in @("unit", "verify", "all") -and (Test-Path -LiteralPath $sharedUiReport)) {
    Write-Host "[harness] Interface compartilhada (Android/JVM): $sharedUiReport"
}
if ($Mode -in @("device", "all") -and (Test-Path -LiteralPath $deviceReport)) {
    Write-Host "[harness] Testes instrumentados: $deviceReport"
}
