package com.faultlabs.burp.faultai.provider;

import com.faultlabs.burp.faultai.config.ProviderType;

public final class ProviderFactory {
    private ProviderFactory() {
    }

    public static AiProvider create(ProviderType type) {
        return switch (type) {
            case OLLAMA -> new OllamaProvider();
            case OPENAI_CODEX -> new OpenAiProvider();
            case ANTHROPIC_CLAUDE -> new AnthropicProvider();
        };
    }
}
