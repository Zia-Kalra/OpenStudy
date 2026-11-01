[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$DbUrl = 'jdbc:mysql://mysql-2ef1d18d-ziakalra13-becd.h.aivencloud.com:13945/openstudy_db?sslMode=VERIFY_IDENTITY',
    [ValidateNotNullOrEmpty()]
    [string]$DbUsername = 'openstudy_team',
    [string]$TruststorePath,
    [switch]$InspectAnalyticsSchema,
    [switch]$UseEnvironmentPasswords
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($TruststorePath)) {
    $TruststorePath = Join-Path $PSScriptRoot 'certs\aiven-truststore.jks'
}

function Read-PrivatePassword([string]$Prompt) {
    $secret = $null
    $pointer = [IntPtr]::Zero
    try {
        $secret = Read-Host $Prompt -AsSecureString
        if ($secret.Length -eq 0) {
            throw "$Prompt must not be empty. Run the script again and enter the password."
        }
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
        if ($null -ne $secret) { $secret.Dispose() }
    }
}

if ($DbUrl -notmatch '^jdbc:mysql://') {
    throw 'DbUrl must be a MySQL JDBC URL.'
}
$sslModes = [regex]::Matches($DbUrl, '(?i)[?&]sslMode=([^&#]*)')
if ($sslModes.Count -ne 1 -or $sslModes[0].Groups[1].Value -cne 'VERIFY_IDENTITY') {
    throw 'DbUrl must contain exactly one sslMode=VERIFY_IDENTITY setting.'
}
if (-not (Test-Path -LiteralPath $TruststorePath -PathType Leaf)) {
    throw 'The truststore file is missing. Supply -TruststorePath pointing to aiven-truststore.jks.'
}
$truststoreFile = (Resolve-Path -LiteralPath $TruststorePath).ProviderPath
$truststoreUrl = ([Uri]$truststoreFile).AbsoluteUri
$wrapper = Join-Path $PSScriptRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw 'mvnw.cmd is missing from the backend directory.'
}

$environmentNames = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'DB_TRUSTSTORE_URL', 'DB_TRUSTSTORE_PASSWORD')
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$locationChanged = $false
try {
    $env:DB_URL = $DbUrl
    $env:DB_USERNAME = $DbUsername
    $env:DB_TRUSTSTORE_URL = $truststoreUrl
    if ($UseEnvironmentPasswords) {
        foreach ($name in @('DB_PASSWORD', 'DB_TRUSTSTORE_PASSWORD')) {
            if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
                throw "Set $name in this PowerShell session before using -UseEnvironmentPasswords."
            }
        }
    }
    else {
        Write-Host 'Paste with Ctrl+V or terminal right-click > Paste, then press Enter. Password input stays hidden.'
        $env:DB_PASSWORD = Read-PrivatePassword 'Aiven DB_PASSWORD (input hidden)'
        $env:DB_TRUSTSTORE_PASSWORD = Read-PrivatePassword 'DB_TRUSTSTORE_PASSWORD (input hidden)'
    }

    $javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java.exe' }
    $truststoreValidator = Join-Path $PSScriptRoot 'tools\ValidateTruststore.java'
    & $javaCommand $truststoreValidator $truststoreFile
    if ($LASTEXITCODE -ne 0) {
        throw 'Startup stopped before connecting to Aiven. Correct DB_TRUSTSTORE_PASSWORD or the local JKS file, then run again. Omit -UseEnvironmentPasswords to enter fresh passwords at the hidden prompts.'
    }

    Push-Location -LiteralPath $PSScriptRoot
    $locationChanged = $true
    if ($InspectAnalyticsSchema) {
        $classpathFile = Join-Path $PSScriptRoot 'target\analytics-inspection.classpath'
        # Resolve runtime libraries only; this goal does not compile or run tests.
        & $wrapper '-q' 'dependency:build-classpath' '-DincludeScope=runtime' "-Dmdep.outputFile=$classpathFile"
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not resolve the runtime libraries for schema inspection.'
        }
        $runtimeClasspath = [IO.File]::ReadAllText($classpathFile).Trim()
        $resourceDirectory = Join-Path $PSScriptRoot 'src\main\resources'
        $inspector = Join-Path $PSScriptRoot 'tools\InspectAnalyticsSchema.java'
        Write-Host 'Inspecting Aiven schema using read-only queries. No application server will be started.'
        & $javaCommand '--class-path' ($resourceDirectory + [IO.Path]::PathSeparator + $runtimeClasspath) $inspector
        if ($LASTEXITCODE -ne 0) {
            throw 'Schema inspection failed. Check the safe diagnostic above; do not share passwords.'
        }
    }
    else {
        Write-Host 'Starting OpenStudy. Keep this terminal open; press Ctrl+C to stop.'
        & $wrapper 'spring-boot:run' '-Dspring-boot.run.profiles=default' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false'
        if ($LASTEXITCODE -ne 0) {
            throw "Spring Boot exited with code $LASTEXITCODE. Check the startup error above."
        }
    }
}
finally {
    if ($locationChanged) { Pop-Location }
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    $previousEnvironment.Clear()
}
