Set-StrictMode -Version Latest

function Test-GeminiKeyConfigured {
    param([AllowNull()][string]$Value)
    return -not [string]::IsNullOrWhiteSpace($Value) -and
        $Value.Trim() -notin @('YOUR_API_KEY_HERE', 'replace_with_your_gemini_api_key', 'your-real-key') -and
        $Value.Length -le 512 -and $Value -notmatch '[\r\n\t]'
}

function Invoke-WithGeminiEnvironment {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$EnvFile, [Parameter(Mandatory)][scriptblock]$Action)

    $names = @('GEMINI_API_KEY', 'GEMINI_MODEL')
    $values = @{}
    $previous = @{}
    if (Test-Path -LiteralPath $EnvFile -PathType Leaf) {
        $lineNumber = 0
        foreach ($line in [System.IO.File]::ReadAllLines($EnvFile)) {
            $lineNumber++
            $text = $line.Trim()
            if ($text.Length -eq 0 -or $text.StartsWith('#')) { continue }
            if ($text -notmatch '^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
                throw "Invalid environment file syntax at line $lineNumber. Expected NAME=value."
            }
            $name = $Matches[1]
            $value = $Matches[2].Trim()
            if ($name -notin $names) { continue }
            if ($values.ContainsKey($name)) { throw "Duplicate Gemini setting at line $lineNumber." }
            if ($value.StartsWith('"') -or $value.StartsWith("'")) {
                $quote = $value.Substring(0, 1)
                $lastQuote = $value.LastIndexOf($quote)
                if ($lastQuote -le 0) { throw "Unclosed quoted Gemini value at line $lineNumber." }
                $suffix = $value.Substring($lastQuote + 1).Trim()
                if ($suffix.Length -gt 0 -and -not $suffix.StartsWith('#')) {
                    throw "Unexpected text after quoted Gemini value at line $lineNumber."
                }
                $value = $value.Substring(1, $lastQuote - 1)
            } else {
                $value = ($value -split '\s+#', 2)[0].Trim()
            }
            # Treat the value strictly as data: no Invoke-Expression, interpolation, or dot-sourcing.
            $values[$name] = $value
        }
    }
    try {
        foreach ($name in $names) {
            $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
            if ([string]::IsNullOrEmpty($previous[$name]) -and $values.ContainsKey($name)) {
                [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process')
            }
        }
        & $Action
    } finally {
        foreach ($name in $previous.Keys) {
            [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
        }
    }
}

Export-ModuleMember -Function Invoke-WithGeminiEnvironment, Test-GeminiKeyConfigured
