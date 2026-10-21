# Gemini API integration — local setup

OpenStudy calls Google's official `models.generateContent` REST endpoint from the
backend. `GeminiAIService` implements the existing `AIService` contract for MCQs and
assignment evaluation; its request/response validators and teacher-review rules are
preserved. The API key is resolved from the `GEMINI_API_KEY` process environment and
sent in the `x-goog-api-key` header, never in a URL or prompt.

The default model is `gemini-3.8-flash`, verified on 13 October 2026 against
https://ai.google.dev/gemini-api/docs/models . Set `GEMINI_MODEL` to select another
supported Gemini model. Official API documentation:
https://ai.google.dev/api/generate-content and https://ai.google.dev/gemini-api/docs/api-key .

## Where to store the key

Store your actual key only in **`C:\Users\athul\OpenStudy\backend\.env`** on this
checkout, or in your process environment. Do not put it in `.env.example`, application
properties, Java, JSP, JavaScript, command-line arguments, screenshots, or chat.

From `C:\Users\athul\OpenStudy`, create the local file if it does not already exist:

```powershell
if (-not (Test-Path -LiteralPath .\backend\.env)) {
    Copy-Item -LiteralPath .\backend\.env.example -Destination .\backend\.env
}
```

Open `backend/.env` in your editor and replace `YOUR_API_KEY_HERE` locally. Keep the
variable names `GEMINI_API_KEY` and `GEMINI_MODEL`. Values may be plain or quoted;
comments and `export NAME=value` are accepted. Shell expressions are never evaluated.

The root ignore rules exclude local `.env` files and database truststores while
allowing `backend/.env.example`. The local file is outside the application resources
and webapp, so it is neither served to browsers nor packaged in the WAR.

## Load the file and start Spring Boot

**Creating `.env` alone does not load it into Java.** Use the provided launcher:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\backend\run-gemini-local.ps1 -CheckConfiguration
powershell -NoProfile -ExecutionPolicy Bypass -File .\backend\run-gemini-local.ps1 -Port 8081
```

Execution-policy bypass applies only to this PowerShell process; it does not change
machine settings. Port 8081 avoids conflict with an existing dashboard on 8080.
Press Ctrl+C to stop. Restart after editing the key/model. The existing teacher pages
are available on the chosen port as well.

The launcher reads only `GEMINI_API_KEY` and `GEMINI_MODEL` from the file. Existing
nonempty process variables take precedence. It passes values to Maven/Java through
the inherited process environment and restores the parent environment on exit.
No value is printed and no secret is passed as an argument. Duplicate or malformed
Gemini settings fail with line-number-only messages. `-EnvFile` can select an alternate
local file; its default is `backend/.env` regardless of your working directory.

The default profiles are **`mock,gemini-local`**: the existing mock profile disables
MySQL; the final local profile enables real Gemini. The server listens only on
127.0.0.1. No Gemini call occurs at startup or during the configuration check.

The existing Aiven properties and truststore handling are unchanged. If you explicitly
want to run against your already-configured database later, the launcher supports
`-WithDatabase`; it uses `gemini-local` without `mock` and preserves the existing
`DB_*` environment variables. It never imports DB settings from the Gemini `.env`.
This option was not used during implementation or tests.

For an IDE launch instead of the helper, configure `GEMINI_API_KEY`/`GEMINI_MODEL`
as run-configuration environment variables and activate `mock,gemini-local`. Do not
store secrets in a shared/tracked IDE configuration. Normal production startup should
use deployment environment variables with `openstudy.ai.provider=gemini` and leave
the diagnostic profile disabled.

## Verify the integration

Use a second terminal; these endpoints accept local command-line requests, not browser
requests. The key is already in the server's environment—do not put it in the request.

Configuration-only check, with **no Google API call**:

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:8081/api/dev/gemini/status
```

`CONFIGURED_NOT_VERIFIED` means a non-placeholder key is loaded and the live provider
is selected; it does not assert that Google accepts the key.

Make one small **live Gemini verification call**:

```powershell
Invoke-RestMethod -Method Post `
    -Uri http://127.0.0.1:8081/api/dev/gemini/test `
    -ContentType 'application/json' `
    -Body '{}'
```

Expected code: `VERIFIED`. This sends a fixed JSON verification prompt, discards model
output, and returns only a success flag, code, static message, and model identifier.
It writes nothing to MySQL. Wait at least 10 seconds between probes. HTTP 404 usually
means an older server is still running or the `gemini-local` profile is absent.

| Code | HTTP | Meaning |
| --- | --- | --- |
| `MISSING_API_KEY` | 503 | Missing/blank/template key; edit the local file and restart |
| `INVALID_API_KEY` | 502 | Google rejected the configured key or restrictions |
| `PROVIDER_NOT_ENABLED` | 503 | Mock provider is still selected |
| `RATE_LIMITED` | 429 | Google quota/rate limit |
| `LOCAL_RATE_LIMITED` | 429 | Wait 10 seconds before another diagnostic probe |
| `TIMEOUT` | 504 | Connection/read timeout; check connectivity and retry later |
| `INVALID_JSON` / `UNEXPECTED_RESPONSE` | 502 | Invalid, blocked, truncated, or unexpected response |
| `LOCAL_ONLY` | 403 | Non-loopback or browser-originated request |

Diagnostics are disabled by default, require the explicit local profile and property,
and are also disabled when `prod` or `production` is active. POST requires JSON, has
no arbitrary prompt parameter, ignores forwarded-address headers, and rejects browser
Origin/Fetch headers. Responses use `no-store`. This is a developer probe, not an
authenticated public AI endpoint; do not expose the local profile through a proxy.

## Transport and secrecy

- Connection timeout: 10 seconds. Read timeout: configurable with
  `openstudy.ai.gemini.timeout-ms` (default 30,000; allowed 1,000–120,000).
- Only the official Google HTTPS base URL is accepted; user-info, query strings,
  alternate hosts and insecure URLs are rejected before credentials are sent.
- API HTTP failures, timeouts, network failures, missing keys, invalid keys, malformed
  output and safety/completion metadata use existing `AiErrorCode` values.
- Configuration `toString()` omits the key, model and URL. Diagnostics do not serialize
  properties, exceptions, causes, generated text, or upstream error bodies.
- HTTP transport logging and detailed error bodies are disabled in the local profile.
  Never enable HTTP tracing/body logging when handling real keys or student data.
- Restrict the API key appropriately in Google AI Studio/Cloud and rotate any key
  that was previously disclosed. AI grades still require teacher review.

## Available checks

```powershell
.\backend\mvnw.cmd -f backend\pom.xml test
powershell -NoProfile -ExecutionPolicy Bypass -File .\backend\src\test\powershell\GeminiLocalEnvironment.Tests.ps1
.\backend\mvnw.cmd -f backend\pom.xml package -DskipTests
```

The automated suite uses synthetic values and mocked Google responses. It covers
request/response validation, missing/invalid keys, HTTP errors, timeouts, safe messages,
diagnostic guards and default-profile isolation. Loader tests verify quoting,
environment precedence, database-variable preservation, literal shell text, restoration
after failures, child inheritance, and actual launcher checks. An additional end-to-end
launcher check loaded a synthetic `.env` into Spring Boot and received HTTP 200 from
the configuration endpoint. No live Gemini verification or Aiven connection was made.
