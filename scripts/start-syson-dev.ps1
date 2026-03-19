$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$localConfigPath = Join-Path $repoRoot 'dev.local.properties'
$jarPath = Join-Path $repoRoot 'backend\application\syson-application\target\syson-application-2026.1.5.jar'
$logDir = Join-Path $repoRoot 'logs'
$stdoutLog = Join-Path $logDir 'syson-backend.out.log'
$stderrLog = Join-Path $logDir 'syson-backend.err.log'

if (-not (Test-Path $jarPath)) {
    throw "Backend jar not found at $jarPath. Build the application first."
}

if (-not (Test-Path $localConfigPath) -and -not $env:SYSON_AGENT_LLM_OPENAI_API_KEY) {
    throw "No OpenAI key configuration found. Create dev.local.properties from dev.local.properties.example or set SYSON_AGENT_LLM_OPENAI_API_KEY."
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

$javaProcesses = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq 'java.exe' -and $_.CommandLine -like '*syson-application-2026.1.5.jar*'
}

foreach ($process in $javaProcesses) {
    Stop-Process -Id $process.ProcessId -Force
}

$arguments = @(
    '-jar',
    $jarPath,
    '--spring.profiles.active=dev'
)

if (Test-Path $localConfigPath) {
    $arguments += "--spring.config.additional-location=optional:file:$localConfigPath"
}

Start-Process -FilePath 'java' `
    -ArgumentList $arguments `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -WindowStyle Hidden

Write-Output "Backend starting on http://localhost:8080"
if (Test-Path $localConfigPath) {
    Write-Output "Loaded local config from $localConfigPath"
} else {
    Write-Output "Using environment variable SYSON_AGENT_LLM_OPENAI_API_KEY"
}
