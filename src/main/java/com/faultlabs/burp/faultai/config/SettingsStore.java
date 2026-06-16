package com.faultlabs.burp.faultai.config;

import burp.api.montoya.persistence.Preferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public final class SettingsStore {
    private static final String SETTINGS_KEY = "faultai.settings.v1";
    private static final String LEGACY_FAULTLENS_SETTINGS_KEY = "faultlens.settings.v1";
    private static final String LEGACY_COMMUNITY_AI_SETTINGS_KEY = "community-ai.settings.v1";

    private final Preferences preferences;
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, String> sessionKeys = new HashMap<>();
    private AppSettings settings;

    public SettingsStore(Preferences preferences) {
        this.preferences = preferences;
        this.settings = load();
    }

    public synchronized AppSettings get() {
        AppSettings copy = settings.copy();
        for (ProviderProfile profile : copy.providerProfiles()) {
            profile.config().apiKey(sessionKeys.getOrDefault(
                    profile.id(),
                    environmentKey(profile.type())
            ));
        }
        return copy;
    }

    public synchronized void save(AppSettings updated) {
        updated.normalize();
        sessionKeys.keySet().removeIf(id -> updated.providerProfile(id) == null);
        for (ProviderProfile profile : updated.providerProfiles()) {
            sessionKeys.put(profile.id(), profile.config().apiKey());
        }
        settings = updated.copy();
        preferences.setString(SETTINGS_KEY, gson.toJson(settings));
    }

    private AppSettings load() {
        String json = preferences.getString(SETTINGS_KEY);
        if (json == null || json.isBlank()) {
            json = preferences.getString(LEGACY_FAULTLENS_SETTINGS_KEY);
        }
        if (json == null || json.isBlank()) {
            json = preferences.getString(LEGACY_COMMUNITY_AI_SETTINGS_KEY);
        }
        if (json == null || json.isBlank()) {
            return AppSettings.defaults();
        }
        try {
            AppSettings loaded = gson.fromJson(json, AppSettings.class);
            loaded.normalize();
            return loaded;
        } catch (RuntimeException ignored) {
            return AppSettings.defaults();
        }
    }

    private static String environmentKey(ProviderType type) {
        String variable = switch (type) {
            case OPENAI_CODEX -> "OPENAI_API_KEY";
            case ANTHROPIC_CLAUDE -> "ANTHROPIC_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
        };
        return System.getenv().getOrDefault(variable, "");
    }
}
