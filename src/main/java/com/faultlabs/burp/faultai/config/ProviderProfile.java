package com.faultlabs.burp.faultai.config;

import java.util.UUID;

public final class ProviderProfile {
    private String id;
    private String name;
    private ProviderType type;
    private ProviderConfig config;

    public ProviderProfile() {
    }

    public ProviderProfile(
            String id,
            String name,
            ProviderType type,
            ProviderConfig config
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.config = config;
    }

    public static ProviderProfile defaultFor(ProviderType type) {
        return switch (type) {
            case OLLAMA -> new ProviderProfile(
                    newId(),
                    "Local Ollama",
                    type,
                    new ProviderConfig("http://localhost:11434", "qwen3:8b", 4_096, 0.2)
            );
            case OPENAI_CODEX -> new ProviderProfile(
                    newId(),
                    "OpenAI Codex",
                    type,
                    new ProviderConfig("https://api.openai.com/v1", "gpt-5.3-codex", 8_192, 0.2)
            );
            case ANTHROPIC_CLAUDE -> new ProviderProfile(
                    newId(),
                    "Claude Sonnet",
                    type,
                    new ProviderConfig("https://api.anthropic.com", "claude-sonnet-4-6", 8_192, 0.2)
            );
        };
    }

    public static ProviderProfile fromLegacy(ProviderType type, ProviderConfig config) {
        ProviderProfile profile = defaultFor(type);
        if (config != null) {
            profile.config = config.copy();
        }
        profile.normalize();
        return profile;
    }

    public ProviderProfile copy() {
        return new ProviderProfile(id, name, type, config == null ? null : config.copy());
    }

    public void normalize() {
        if (id == null || id.isBlank()) {
            id = newId();
        }
        if (type == null) {
            type = ProviderType.OLLAMA;
        }
        ProviderProfile defaults = defaultFor(type);
        if (config == null) {
            config = defaults.config.copy();
        }
        if (config.endpoint().isBlank()) {
            config.endpoint(defaults.config.endpoint());
        }
        if (config.model().isBlank()) {
            config.model(defaults.config.model());
        }
        if (config.maxOutputTokens() <= 0) {
            config.maxOutputTokens(defaults.config.maxOutputTokens());
        }
        if (name == null || name.isBlank()) {
            name = defaults.name;
        }
    }

    public String id() {
        return id == null ? "" : id;
    }

    public void id(String id) {
        this.id = id;
    }

    public String name() {
        return name == null ? "" : name;
    }

    public void name(String name) {
        this.name = name;
    }

    public ProviderType type() {
        return type == null ? ProviderType.OLLAMA : type;
    }

    public void type(ProviderType type) {
        this.type = type;
    }

    public ProviderConfig config() {
        normalize();
        return config;
    }

    @Override
    public String toString() {
        ProviderConfig profileConfig = config();
        return name() + " (" + type() + " / " + profileConfig.model() + ")";
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
