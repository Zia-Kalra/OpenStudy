# Retired Gemini local setup

Gemini has been replaced by Groq. Both MCQ generation and suggested evaluation
are implemented by the shared Groq AIService adapter, and the integrated
assignment module also supports Groq attachment processing.

The Gemini provider, parser, configuration, local diagnostics, launcher, and
provider-specific tests were retired together during the integration merge.
Use [Groq configuration](groq-configuration.md) and the ignored VS Code launch
configuration or `backend/run-backend.ps1`. The Gemini-local profile and commands
are no longer supported. Existing local secret files are untouched.
