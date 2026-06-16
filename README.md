# FaultAI for Burp

FaultAI is a FaultLabs Burp Suite Community Edition extension that adds an
AI-assisted HTTP testing workflow using infrastructure you control.

It is inspired by the interaction model of Burp AI, but it does not use or bypass
PortSwigger's Professional-only AI API.

## Features

- Dedicated `FaultAI` workspace inside Burp.
- Repeater-style conversation tabs for separate investigations.
- Right-click conversation tab menu for rename, clear, new tab, and close.
- Per-tab model profile selection, so each conversation can use a different LLM.
- Named model profiles for multiple endpoints, providers, and models.
- Ollama support with configurable local or remote endpoints.
- OpenAI Responses API support for Codex-capable models.
- Anthropic Messages API support for Claude models.
- One-click model profile switching per conversation tab without restarting Burp.
- Clear the active conversation without removing other tabs.
- Context-menu actions on HTTP requests and responses:
  - Send to chat
  - Analyze security
  - Explain
  - Suggest test cases
- Security-focused prompt templates with evidence and false-positive guidance.
- Configurable system prompt, context limit, output limit, and temperature.
- Secret redaction enabled by default.
- Add, duplicate, remove, rename, and test model profiles from Settings.
- Settings persisted through Burp; API keys remain memory-only per profile.
- `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, and `OLLAMA_API_KEY` environment
  variable fallback.

## Requirements

- Burp Suite Community Edition or Professional.
- Java 17 or newer.
- At least one configured model profile using:
  - Ollama
  - OpenAI API
  - Anthropic API

An OpenAI API key is separate from a ChatGPT or Codex subscription. An Anthropic
API key is separate from a Claude or Claude Code subscription. This extension
does not read credentials from the Codex CLI or Claude Code CLI.

## Build

```bash
./gradlew clean test jar
```

The extension JAR is generated at:

```text
build/libs/faultai-for-burp-0.1.0.jar
```

## Install

1. Open Burp Suite.
2. Go to `Extensions` > `Installed`.
3. Click `Add`.
4. Select extension type `Java`.
5. Select `build/libs/faultai-for-burp-0.1.0.jar`.
6. Open the new `FaultAI` tab and click `Settings`.

## Conversations and model profiles

FaultAI opens with one conversation tab. Use:

- `New tab` to start another independent conversation.
- `Clear` to erase only the active conversation.
- `Close tab` to close the active conversation. Closing the last tab clears it.
- `Model profile` to choose the model used by the active tab.

Right-click a conversation tab to rename it, clear it, open a new tab, or close it.

Each tab keeps its own conversation history and selected model profile. This
lets you compare outputs across providers, for example one tab on local Ollama,
one on OpenAI/Codex, and one on Claude.

## Model profiles

Open `FaultAI` > `Settings` to manage model profiles. A profile contains:

- Display name
- Provider type
- Endpoint
- Model
- Optional API key
- Output token limit
- Temperature

You can create multiple profiles for the same provider. For example:

- `Local Qwen` using Ollama model `qwen3:8b`
- `Local Llama` using Ollama model `llama3.1:8b`
- `Codex` using OpenAI model `gpt-5.3-codex`
- `Claude` using Anthropic model `claude-sonnet-4-6`

Choose the active profile for each conversation from the `Model profile`
dropdown in the main tab.

## Provider setup

### Ollama

Default endpoint:

```text
http://localhost:11434
```

Set the model to one installed on the Ollama server, for example:

```bash
ollama list
```

For a remote Ollama instance, replace the endpoint with its HTTPS base URL.
The optional API-key field is sent as a Bearer token for gateways that protect
the Ollama server.

### OpenAI / Codex

Default endpoint:

```text
https://api.openai.com/v1
```

The default OpenAI profile uses `gpt-5.3-codex`, but the model field is editable.
Configure an API key in the settings dialog or start Burp with
`OPENAI_API_KEY` in its environment.

### Anthropic / Claude

Default endpoint:

```text
https://api.anthropic.com
```

The default Claude profile uses `claude-sonnet-4-6`, but the model field is editable.
Configure an API key in the settings dialog or start Burp with
`ANTHROPIC_API_KEY` in its environment.

## Privacy and safety

HTTP traffic may contain passwords, session cookies, API keys, personal data,
or proprietary application content. Review provider retention policies before
sending traffic to a remote service.

Secret redaction removes common credential headers, bearer tokens, JWTs, JSON
secret fields, and form secret fields. It is intentionally conservative and
cannot guarantee that every sensitive value will be detected.

Only test systems you are authorized to assess. AI output is untrusted guidance:
validate every claim and payload before using it.

## Current limitations

- Community Edition does not expose Burp Scanner, native Burp AI, or
  `Explore Issue`, so autonomous scanner-finding investigation is unavailable.
- Responses are currently delivered when complete rather than token-streamed.
- Conversation tabs are in-memory and are reset when Burp unloads the extension.
- API keys are not persisted. Re-enter per-profile keys after restarting Burp
  unless an environment variable is configured.
- The extension does not automatically apply generated HTTP messages back into
  Repeater. This avoids silently replacing requests with unverified model output.

## Development

The extension uses:

- Montoya API `2026.4`
- Gson for provider JSON
- JUnit 5
- Java's built-in HTTP client

Provider implementations are isolated under
`src/main/java/com/faultlabs/burp/faultai/provider`.
