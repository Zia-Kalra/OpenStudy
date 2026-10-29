# Teacher AI configuration: Groq

Normal startup selects `openstudy.ai.provider=groq`. The database-independent mock
profile explicitly selects mock AI. `OPENSTUDY_AI_PROVIDER` takes precedence over
`AI_PROVIDER`; both branch conventions remain supported. Valid choices are `groq`
and `mock`. Gemini is retired.

Set credentials only in your ignored local `.vscode/launch.json` or terminal:

- `GROQ_API_KEY`: private backend-only API key.
- `GROQ_MODEL`: shared text model, default `openai/gpt-oss-20b`.
- `GROQ_VISION_MODEL`: Member 3 assignment attachment model, default `qwen/qwen3.8-27b`.

Restart **OpenStudy backend** with F5 after changing local values. The PowerShell
launcher also inherits Groq variables. Aiven credentials, `DB_TRUSTSTORE_URL` and
TLS `VERIFY_IDENTITY` are unchanged. Local credentials and truststores are ignored
by Git. No API request happens at startup.

There is one `GroqProperties`, one conditional `GroqRestClientConfig`, and one
`GroqAIService` implementing the shared `AIService` contract. It combines Member
5's transport/key validation, explicit bean qualifier, bounded output and completed
response checks with integration's `GroqJsonParser`, prompt construction and safe
error mapping. The configured base URL is restricted to the official HTTPS API
endpoint, and the timeout is validated (default 60 seconds). A missing key blocks
only an explicitly invoked AI operation. API keys and upstream bodies are never
logged by this adapter. Generated content is labeled `GROQ`; suggested grades
retain mandatory teacher review, with no silent fallback to mock content.

Member 3's separate `AssignmentAiService` is preserved. It is a module-specific
service, not a second shared AIService bean. It consumes the same key/text-model
properties and the vision-model setting, preserving image/PDF processing, retries,
question generation and assignment grading. Its controllers and access checks
are unchanged by the merge. The shared adapter also supports the Member 5 class/student insight UI; it selects anonymous calculated fact IDs and eligible advice, never generated grade values. See [Teacher Analytics](teacher-analytics.md).

Protocol references:
[Groq API reference](https://console.groq.com/docs/api-reference),
[supported models](https://console.groq.com/docs/models).
