Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$backendRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
Import-Module (Join-Path $backendRoot 'scripts/GeminiLocalEnvironment.psm1') -Force
$fixtureDir = Join-Path $backendRoot 'target/gemini-env-tests'
[IO.Directory]::CreateDirectory($fixtureDir) | Out-Null
$fixture = Join-Path $fixtureDir 'fixture.env'
$savedKey = [Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process')
$savedModel = [Environment]::GetEnvironmentVariable('GEMINI_MODEL', 'Process')
$savedDb = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'Process')
$passed = 0

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}
function Write-Fixture([string]$Text) { [IO.File]::WriteAllText($fixture, $Text) }
function Reset-Gemini {
    [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', $null, 'Process')
    [Environment]::SetEnvironmentVariable('GEMINI_MODEL', $null, 'Process')
}

try {
    Reset-Gemini
    [Environment]::SetEnvironmentVariable('DB_PASSWORD', 'SYNTHETIC_DB_SENTINEL', 'Process')
    Write-Fixture "# Example`nGEMINI_API_KEY=SYNTHETIC_FILE_KEY`nGEMINI_MODEL=gemini-3.8-flash`nDB_PASSWORD=DO_NOT_LOAD"
    Invoke-WithGeminiEnvironment $fixture {
        Assert-True ($env:GEMINI_API_KEY -eq 'SYNTHETIC_FILE_KEY') 'File key was not loaded.'
        Assert-True ($env:GEMINI_MODEL -eq 'gemini-3.8-flash') 'Model was not loaded.'
        Assert-True ($env:DB_PASSWORD -eq 'SYNTHETIC_DB_SENTINEL') 'Database environment was changed.'
    }
    Assert-True ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process'))) 'Key was not restored.'
    $passed++

    [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', 'SYNTHETIC_EXISTING_KEY', 'Process')
    Invoke-WithGeminiEnvironment $fixture {
        Assert-True ($env:GEMINI_API_KEY -eq 'SYNTHETIC_EXISTING_KEY') 'Existing environment must take precedence.'
    }
    Assert-True ($env:GEMINI_API_KEY -eq 'SYNTHETIC_EXISTING_KEY') 'Existing key was changed.'
    $passed++

    Reset-Gemini
    Write-Fixture "export GEMINI_API_KEY='SYNTHETIC_QUOTED_KEY' # comment`nGEMINI_MODEL=`"gemini-3.8-flash`""
    Invoke-WithGeminiEnvironment $fixture {
        Assert-True ($env:GEMINI_API_KEY -eq 'SYNTHETIC_QUOTED_KEY') 'Quoted key was not parsed.'
        Assert-True ($env:GEMINI_MODEL -eq 'gemini-3.8-flash') 'Quoted model was not parsed.'
    }
    $passed++

    Write-Fixture 'GEMINI_API_KEY=$(throw "MUST_NOT_EXECUTE")'
    Invoke-WithGeminiEnvironment $fixture {
        Assert-True ($env:GEMINI_API_KEY.StartsWith('$(')) 'Shell text was evaluated or expanded.'
    }
    $passed++

    Write-Fixture "GEMINI_API_KEY=SYNTHETIC_FILE_KEY`nGEMINI_API_KEY=SYNTHETIC_DUPLICATE"
    $rejected = $false
    try { Invoke-WithGeminiEnvironment $fixture { throw 'Unexpected action execution.' } } catch {
        $rejected = $_.Exception.Message -eq 'Duplicate Gemini setting at line 2.'
    }
    Assert-True $rejected 'Duplicate secret settings were not rejected safely.'
    $passed++

    Write-Fixture "GEMINI_API_KEY='SYNTHETIC_UNCLOSED"
    $rejected = $false
    try { Invoke-WithGeminiEnvironment $fixture { throw 'Unexpected action execution.' } } catch {
        $rejected = $_.Exception.Message -eq 'Unclosed quoted Gemini value at line 1.'
    }
    Assert-True $rejected 'Malformed quoted values were not rejected safely.'
    $passed++

    Write-Fixture 'GEMINI_API_KEY=SYNTHETIC_FILE_KEY'
    try { Invoke-WithGeminiEnvironment $fixture { throw 'Synthetic action failure.' } } catch { }
    Assert-True ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process'))) 'Key remained after action failure.'
    $passed++

    $childCheck = Join-Path $fixtureDir 'child-check.ps1'
    [IO.File]::WriteAllText($childCheck, 'if ($env:GEMINI_API_KEY -eq ''SYNTHETIC_FILE_KEY'') { exit 0 } else { exit 1 }')
    Invoke-WithGeminiEnvironment $fixture {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $childCheck
        Assert-True ($LASTEXITCODE -eq 0) 'Child process did not inherit the key.'
    }
    $passed++

    Assert-True (-not (Test-GeminiKeyConfigured 'YOUR_API_KEY_HERE')) 'Placeholder was accepted.'
    Assert-True (-not (Test-GeminiKeyConfigured $null)) 'Missing key was accepted.'
    Assert-True (Test-GeminiKeyConfigured 'SYNTHETIC_FILE_KEY') 'Non-placeholder was rejected.'
    $passed++

    # Exercise the actual launcher, using a temporary copied backend and synthetic .env.
    $launcherDir = Join-Path $fixtureDir 'launcher'
    [IO.Directory]::CreateDirectory((Join-Path $launcherDir 'scripts')) | Out-Null
    Copy-Item -LiteralPath (Join-Path $backendRoot 'run-gemini-local.ps1') -Destination $launcherDir
    Copy-Item -LiteralPath (Join-Path $backendRoot 'scripts/GeminiLocalEnvironment.psm1') -Destination (Join-Path $launcherDir 'scripts')
    [IO.File]::WriteAllText((Join-Path $launcherDir '.env'), "GEMINI_API_KEY=SYNTHETIC_LAUNCHER_KEY`nGEMINI_MODEL=gemini-3.8-flash")
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $launcherDir 'run-gemini-local.ps1') -CheckConfiguration 2>&1
    Assert-True ($LASTEXITCODE -eq 0) 'Launcher configuration check failed.'
    Assert-True (($output -join "`n") -notmatch 'SYNTHETIC_LAUNCHER_KEY') 'Launcher output disclosed a key.'
    $passed++

    Write-Host "Gemini local environment tests: $passed passed. Synthetic values only; no API or database calls."
} finally {
    [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', $savedKey, 'Process')
    [Environment]::SetEnvironmentVariable('GEMINI_MODEL', $savedModel, 'Process')
    [Environment]::SetEnvironmentVariable('DB_PASSWORD', $savedDb, 'Process')
}
