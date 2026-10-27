# Backend and Aiven verification (Windows PowerShell)

The `mock` profile deliberately has no DataSource or JdbcTemplate. Its tests and
startup do not verify Aiven. Normal startup also does not prove connectivity:
Hikari opens its pool when a JDBC operation requests a connection.

`GET /api/db-test` is available only without `mock`. It executes `SELECT 1`, then
`SELECT DATABASE()`, and returns the selected database name. An HTTP 200 response
containing `openstudy_db` proves those read-only queries completed through the
application's configured DataSource. Database failures use the existing sanitized
error response. Do not use signup, password reset, or question-bank writes as
connectivity checks.

## Start with private password prompts

For the current shared Aiven service, run:

```powershell
Set-Location C:\Users\athul\OpenStudy\backend
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-backend.ps1
```

Paste or type the existing Aiven user password at the first prompt and the
existing JKS truststore password at the second, pressing Enter after each. Input
is hidden; no password is written to a configuration file or placed in command
arguments. Empty input is rejected. The execution-policy override applies only
to this PowerShell process.

The launcher sets the five database variables in the same process that launches
Maven, uses the supplied Aiven hostname/port and `openstudy_team`, and derives the
truststore URL from the actual file `certs/aiven-truststore.jks`. On this checkout
the URI is `file:///C:/Users/athul/OpenStudy/backend/certs/aiven-truststore.jks`;
the `certs` directory by itself is not a truststore file. The defaults can be
overridden with `-DbUrl`, `-DbUsername`, and `-TruststorePath`; TLS must remain
`sslMode=VERIFY_IDENTITY`.

Keep the terminal open while using the backend; Ctrl+C stops it. The launcher
disables DevTools restart for that run so startup errors exit with a failure
status instead of being hidden behind a successful Maven run. Other launch
methods retain their existing DevTools behavior. See the official
[Spring Boot DevTools instructions](https://docs.spring.io/spring-boot/reference/using/devtools.html)
for the JVM property used by the launcher.

If startup says `Failed to determine suitable jdbc url`, no usable URL reached
Spring. Variables entered in a different PowerShell process are not inherited
automatically. This failure happens before credentials or TLS can be verified.
The launcher supplies the URL and both password prompts before starting Maven.

After Tomcat starts, open `http://localhost:8080/login` and request
`http://localhost:8080/api/db-test`. Only a successful database response containing
`openstudy_db` confirms the real JDBC check. A successful startup alone is not
sufficient.

## Tests and database-independent startup

From the repository root:

```powershell
Set-Location C:\Users\athul\OpenStudy\backend
.\mvnw.cmd clean test
if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' }
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=mock'
```

In a second PowerShell window:

```powershell
(Invoke-WebRequest -UseBasicParsing http://localhost:8080/).StatusCode
(Invoke-WebRequest -UseBasicParsing http://localhost:8080/login).StatusCode
```

Both should return `200`. Stop the mock server with Ctrl+C before starting the
database-backed application on the same port. `/api/db-test` returns 404 under
`mock` by design.

## Configure and start the database-backed application

Run this in the PowerShell window that will launch Maven. Take the exact hostname
and port from the shared Aiven service's connection information. Use its hostname
rather than an IP address so TLS hostname verification can succeed. The local
`backend/.env` file is not automatically imported by Spring Boot or Maven.

```powershell
Set-Location C:\Users\athul\OpenStudy\backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4.1'

$aivenHost = Read-Host 'Aiven MySQL hostname'
$aivenPort = Read-Host 'Aiven MySQL port'
$env:DB_URL = "jdbc:mysql://${aivenHost}:${aivenPort}/openstudy_db?sslMode=VERIFY_IDENTITY"
$env:DB_USERNAME = 'openstudy_team'
$truststoreFile = (Resolve-Path -LiteralPath .\certs\aiven-truststore.jks -ErrorAction Stop).Path
$env:DB_TRUSTSTORE_URL = ([Uri]$truststoreFile).AbsoluteUri

function Read-ProcessSecret([string]$Name) {
    $secret = Read-Host $Name -AsSecureString
    $secretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    try {
        [Environment]::SetEnvironmentVariable(
            $Name,
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer),
            'Process'
        )
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer)
        $secret.Dispose()
    }
}

Read-ProcessSecret DB_PASSWORD
Read-ProcessSecret DB_TRUSTSTORE_PASSWORD

# Verify the JKS with its existing password without putting the password in arguments.
& "$env:JAVA_HOME\bin\keytool.exe" -list -storetype JKS -keystore $truststoreFile -storepass:env DB_TRUSTSTORE_PASSWORD
if ($LASTEXITCODE -ne 0) { throw 'Truststore verification failed.' }

Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=default' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false'
```

Keep `VERIFY_IDENTITY`. The application uses `DB_TRUSTSTORE_URL`, JKS, and
`spring.sql.init.mode=never`; no schema or seed SQL runs on startup. If the JKS is
missing, restore the existing team truststore locally or create a local truststore
from the CA certificate downloaded from the correct Aiven service, using JDK
`keytool -importcert`. Use the password belonging to that truststore. Do not change
the shared database user's password.

In a second PowerShell window, after the startup log says Tomcat started:

```powershell
(Invoke-WebRequest -UseBasicParsing http://localhost:8080/login).StatusCode
$dbCheck = Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/db-test
if ($dbCheck.StatusCode -ne 200 -or $dbCheck.Content.Trim() -ne 'openstudy_db') {
    throw 'The application has not verified the expected Aiven database.'
}
'PASS: application SELECT 1 and SELECT DATABASE() completed against openstudy_db.'
```

Confirm the Hikari startup log includes pool initialization and an added
connection. A Maven `BUILD SUCCESS` line alone is insufficient; devtools can let
the Maven run goal finish successfully even when application startup failed.

## Failure interpretation

`run-backend.ps1` validates the local JKS password before starting Maven or the
schema inspector, using `tools/ValidateTruststore.java`. It reads the password
from the process environment, prints no credentials, and changes neither the
truststore nor the database. An `IOException -> UnrecoverableKeyException`
failure during TLS setup indicates that the supplied password cannot unlock the
JKS or that its integrity check failed. Use the existing truststore password from
the successful schema inspection; it is the password set when the JKS was created.
Restart the server after correcting it. To avoid reusing an incorrect environment
value, launch `run-backend.ps1` without `-UseEnvironmentPasswords` and enter both
passwords at the hidden prompts. A failed database login cannot establish the
session required by Teacher Analytics, so a redirect to login is expected then.

| Evidence | Meaning / local action |
| --- | --- |
| DataSource URL missing | Set `DB_URL` in the launching PowerShell process. |
| MySQL 1045 / access denied | Verify `DB_USERNAME` and the existing `DB_PASSWORD` locally. |
| Keystore missing, wrong password, or certificate validation failure | Check the file URI, JKS password, service CA, and certificate validity. |
| Hostname verification failure | Use the Aiven hostname from the certificate/service configuration. |
| Unknown host, connection timeout, or refused connection | Check Aiven hostname/port, DNS, network access, and service availability. |
| 404 from `/api/db-test` | Check that `mock` is inactive and the updated backend is running. |
| HTTP 503 from `/api/db-test` | JDBC failed; find the console line `JDBC connection failure ref=... causes=...`. It prints only exception types, SQLState, and vendor error codes; share that line instead of raw debug logs. |
| Port 8080 already in use | Inspect `Get-NetTCPConnection -LocalPort 8080 -State Listen`; stop your old server or use another port. |

The connection failure diagnostic identifies MySQL error `1045` / SQLState
`28000` as authentication rejection, `1049` as an unknown database, and
`UnknownHostException` as DNS failure. `SSLHandshakeException` and certificate
validation exceptions point to TLS; `ConnectException` and
`SocketTimeoutException` point to network or service reachability. Some failures
need a separate local truststore check to narrow them further. Keep
`VERIFY_IDENTITY` enabled. Hikari DEBUG logging is unnecessary and can print
connection configuration.

If this execution environment reports Maven's repository as `C:\.m2\repository`,
point Maven at the existing user cache before rerunning the same commands:

```powershell
$env:MAVEN_OPTS = "$env:MAVEN_OPTS `"-Dmaven.repo.local=$env:USERPROFILE\.m2\repository`""
```

For driver property definitions, see the official
[MySQL Connector/J security documentation](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).
