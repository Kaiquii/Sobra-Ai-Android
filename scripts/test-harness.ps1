[CmdletBinding()]
param(
    [ValidateSet("unit", "verify", "device", "all")]
    [string] $Mode = "unit",

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

Write-Host "[harness] Executando modo '$Mode'..."
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
$lintReport = Join-Path $projectRoot "app\build\reports\lint-results-debug.html"
$deviceReport = Join-Path $projectRoot "app\build\reports\androidTests\connected\debug\index.html"

Write-Host "[harness] OK: modo '$Mode' concluido."
if ($Mode -in @("unit", "verify", "all") -and (Test-Path -LiteralPath $unitReport)) {
    Write-Host "[harness] Testes unitarios: $unitReport"
}
if ($Mode -in @("verify", "all") -and (Test-Path -LiteralPath $lintReport)) {
    Write-Host "[harness] Lint: $lintReport"
}
if ($Mode -in @("device", "all") -and (Test-Path -LiteralPath $deviceReport)) {
    Write-Host "[harness] Testes instrumentados: $deviceReport"
}
