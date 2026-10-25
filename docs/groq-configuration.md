# Teacher AI configuration: Groq

Normal startup now selects `openstudy.ai.provider=groq`. The existing mock profile
still selects mock AI. Set `AI_PROVIDER=mock` for a deliberate demo, or
`AI_PROVIDER=gemini` to use the preserved legacy integration.

The ignored `.vscode/launch.json` already passes these environment variables:

- `GROQ_API_KEY`: your private Groq API key. It stays server-side.
- `GROQ_MODEL`: the text model, default `openai/gpt-oss-20b`.

Fill these values locally and restart **OpenStudy backend** with F5. Database
credentials, `DB_TRUSTSTORE_URL`, and `VERIFY_IDENTITY` are unchanged. The PowerShell
launcher also inherits Groq variables from its launching terminal.

`GROQ_VISION_MODEL` is not consumed by this text-only adapter. Its presence does not
activate image processing. No new teacher AI page, analytics explanation feature,
or public AI endpoint is added in this change.

`GroqAIService` implements the existing `AIService` methods for MCQ drafts and
suggested assignment evaluation. It uses Spring RestClient, the official fixed
HTTPS Chat Completions endpoint, bearer authentication, JSON object responses,
bounded timeouts, and existing request/response validators. Output is labeled
`GROQ`; grades retain mandatory teacher review. The service performs no database
writes. There is no automatic fallback to mock content on an API failure.

No API request happens at startup. A missing key fails only an explicitly invoked
AI operation. API keys, prompts, student answers and upstream response bodies are
not logged by the adapter; upstream exceptions are replaced by safe error codes.
Keep HTTP wire/configuration debug logging disabled when using real credentials.

Gemini-specific code remains available only as legacy functionality:
`GeminiAIService`, `GeminiProperties`, `GeminiRestClientConfig`,
`GeminiJsonParser`, the opt-in diagnostics and `gemini-local` launcher/profile.
The Gemini HTTP client is now conditional on the Gemini provider, so it is not
created for normal Groq startup. Neither provider needs a vendor SDK dependency;
the Maven dependencies are unchanged.

Validation for this change: compilation and default backend startup passed. A
read-only request to Groq's model-list endpoint accepted the locally configured
key and included the configured text model. No generation call was made, so
live MCQ/evaluation output remains unverified. No student data was sent to Groq.

Protocol references:
[Groq API reference](https://console.groq.com/docs/api-reference),
[supported models](https://console.groq.com/docs/models).
