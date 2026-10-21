[CmdletBinding()]
param(
    [switch]$WithDatabase,
    [switch]$CheckConfiguration,
    [string]$EnvFile,
    [ValidateRange(1024, 65535)][int]$Port = 8080
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'scripts/GeminiLocalEnvironment.psm1') -Force
if ([string]::IsNullOrWhiteSpace($EnvFile)) { $EnvFile = Join-Path $PSScriptRoot '.env' }
$runExitCode = 0

try {
    Invoke-WithGeminiEnvironment -EnvFile $envFile -Action {
        $key = [Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process')
        if (-not (Test-GeminiKeyConfigured $key)) {
            throw 'GEMINI_API_KEY is missing or still a placeholder. Copy backend/.env.example to backend/.env and replace the placeholder locally.'
        }
        $model = [Environment]::GetEnvironmentVariable('GEMINI_MODEL', 'Process')
        if (-not [string]::IsNullOrEmpty($model) -and $model -notmatch '^gemini-[A-Za-z0-9._-]{1,90}$') {
            throw 'GEMINI_MODEL must be a Gemini model identifier, not a URL or credential.'
        }
        if ($CheckConfiguration) {
            Write-Host 'Gemini environment loaded successfully. API key value is hidden. No server, database, or API request was started.'
            return
        }
        $profiles = if ($WithDatabase) { 'gemini-local' } else { 'mock,gemini-local' }
        $arguments = @(
            '-f', (Join-Path $PSScriptRoot 'pom.xml'),
            'spring-boot:run',
            "-Dspring-boot.run.profiles=$profiles",
            '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false',
            "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=$Port"
        )
        # The key is inherited in the child environment, never passed as a command-line argument.
        Push-Location $PSScriptRoot
        try {
            & (Join-Path $PSScriptRoot 'mvnw.cmd') @arguments
            if ($LASTEXITCODE -ne 0) { throw 'The local application did not start or exited unsuccessfully. Check the application output above.' }
        } finally {
            Pop-Location
        }
    }
} catch {
    # Errors emitted by this launcher contain only fixed messages, field names and line numbers.
    Write-Host ('Gemini local setup failed: ' + $_.Exception.Message)
    $runExitCode = 1
}
exit $runExitCode
