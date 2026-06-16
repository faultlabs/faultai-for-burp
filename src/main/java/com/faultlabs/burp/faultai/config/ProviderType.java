package com.faultlabs.burp.faultai.config;

public enum ProviderType {
    OLLAMA("Ollama"),
    OPENAI_CODEX("OpenAI / Codex"),
    ANTHROPIC_CLAUDE("Anthropic / Claude");

    private final String displayName;

    ProviderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
