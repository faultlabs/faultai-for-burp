package com.faultlabs.burp.faultai.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AppSettings {
    public static final String DEFAULT_SYSTEM_PROMPT = """
            You are an expert web application security testing assistant embedded in Burp Suite.
            Help analyze HTTP traffic, explain web technologies, identify plausible vulnerabilities,
            and design focused validation steps for systems the user is authorized to test.
            Separate observations from hypotheses. Do not claim a vulnerability is confirmed without
            evidence. Prefer concise, reproducible steps and mention important false-positive checks.
            Never invent request or response details that are not present in the supplied context.
            """;

    private String selectedProviderId;
    private List<ProviderProfile> providerProfiles = defaultProviderProfiles();
    private boolean redactSecrets = true;
    private boolean includeResponse = true;
    private int maxContextCharacters = 80_000;
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    @Deprecated
    private ProviderType selectedProvider;

    @Deprecated
    private Map<ProviderType, ProviderConfig> providers;

    public static AppSettings defaults() {
        return new AppSettings();
    }

    public AppSettings copy() {
        AppSettings copy = new AppSettings();
        copy.selectedProviderId = selectedProviderId;
        copy.redactSecrets = redactSecrets;
        copy.includeResponse = includeResponse;
        copy.maxContextCharacters = maxContextCharacters;
        copy.systemPrompt = systemPrompt;
        copy.providerProfiles = new ArrayList<>();
        for (ProviderProfile profile : providerProfiles()) {
            copy.providerProfiles.add(profile.copy());
        }
        return copy;
    }

    public void normalize() {
        if (providers != null && !providers.isEmpty()) {
            providerProfiles = new ArrayList<>();
            for (ProviderType type : ProviderType.values()) {
                providerProfiles.add(ProviderProfile.fromLegacy(type, providers.get(type)));
            }
            if (selectedProvider != null) {
                selectedProviderId = providerProfiles.stream()
                        .filter(profile -> profile.type() == selectedProvider)
                        .findFirst()
                        .map(ProviderProfile::id)
                        .orElse(null);
            }
            providers = null;
            selectedProvider = null;
        }

        if (providerProfiles == null || providerProfiles.isEmpty()) {
            providerProfiles = defaultProviderProfiles();
        }

        Set<String> seenIds = new HashSet<>();
        for (ProviderProfile profile : providerProfiles) {
            profile.normalize();
            if (seenIds.contains(profile.id())) {
                profile.id("");
                profile.normalize();
            }
            seenIds.add(profile.id());
        }

        if (selectedProviderId == null || selectedProviderId.isBlank()
                || providerProfile(selectedProviderId) == null) {
            selectedProviderId = providerProfiles.get(0).id();
        }

        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = DEFAULT_SYSTEM_PROMPT;
        }
        maxContextCharacters = Math.max(4_000, maxContextCharacters);
    }

    public List<ProviderProfile> providerProfiles() {
        normalize();
        return providerProfiles;
    }

    public ProviderProfile selectedProviderProfile() {
        normalize();
        ProviderProfile profile = providerProfile(selectedProviderId);
        return profile == null ? providerProfiles.get(0) : profile;
    }

    public ProviderProfile providerProfile(String id) {
        if (providerProfiles == null) {
            return null;
        }
        for (ProviderProfile profile : providerProfiles) {
            if (profile.id().equals(id)) {
                return profile;
            }
        }
        return null;
    }

    public String selectedProviderId() {
        normalize();
        return selectedProviderId;
    }

    public void selectedProviderId(String selectedProviderId) {
        this.selectedProviderId = selectedProviderId;
    }

    public ProviderProfile addProviderProfile(ProviderType type) {
        normalize();
        ProviderProfile profile = ProviderProfile.defaultFor(type);
        String baseName = profile.name();
        int suffix = 2;
        while (hasProfileName(profile.name())) {
            profile.name(baseName + " " + suffix++);
        }
        providerProfiles.add(profile);
        selectedProviderId = profile.id();
        return profile;
    }

    public ProviderProfile duplicateProviderProfile(String id) {
        normalize();
        ProviderProfile original = providerProfile(id);
        if (original == null) {
            return addProviderProfile(ProviderType.OLLAMA);
        }
        ProviderProfile copy = original.copy();
        copy.id("");
        copy.name(original.name() + " copy");
        copy.normalize();
        providerProfiles.add(copy);
        selectedProviderId = copy.id();
        return copy;
    }

    public void removeProviderProfile(String id) {
        normalize();
        if (providerProfiles.size() <= 1) {
            return;
        }
        providerProfiles.removeIf(profile -> profile.id().equals(id));
        if (providerProfile(selectedProviderId) == null) {
            selectedProviderId = providerProfiles.get(0).id();
        }
    }

    public boolean redactSecrets() {
        return redactSecrets;
    }

    public void redactSecrets(boolean redactSecrets) {
        this.redactSecrets = redactSecrets;
    }

    public boolean includeResponse() {
        return includeResponse;
    }

    public void includeResponse(boolean includeResponse) {
        this.includeResponse = includeResponse;
    }

    public int maxContextCharacters() {
        return maxContextCharacters;
    }

    public void maxContextCharacters(int maxContextCharacters) {
        this.maxContextCharacters = maxContextCharacters;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public void systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    private boolean hasProfileName(String name) {
        for (ProviderProfile profile : providerProfiles) {
            if (profile.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static List<ProviderProfile> defaultProviderProfiles() {
        List<ProviderProfile> profiles = new ArrayList<>();
        profiles.add(ProviderProfile.defaultFor(ProviderType.OLLAMA));
        profiles.add(ProviderProfile.defaultFor(ProviderType.OPENAI_CODEX));
        profiles.add(ProviderProfile.defaultFor(ProviderType.ANTHROPIC_CLAUDE));
        return profiles;
    }

}
