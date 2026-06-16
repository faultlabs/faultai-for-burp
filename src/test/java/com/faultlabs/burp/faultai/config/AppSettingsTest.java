package com.faultlabs.burp.faultai.config;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsTest {
    @Test
    void defaultsCreateProfilesForEveryProvider() {
        AppSettings settings = AppSettings.defaults();

        Set<ProviderType> types = settings.providerProfiles().stream()
                .map(ProviderProfile::type)
                .collect(Collectors.toSet());

        assertEquals(EnumSet.allOf(ProviderType.class), types);
        assertEquals(ProviderType.OLLAMA, settings.selectedProviderProfile().type());
        for (ProviderProfile profile : settings.providerProfiles()) {
            assertTrue(!profile.name().isBlank());
            assertTrue(!profile.config().endpoint().isBlank());
            assertTrue(!profile.config().model().isBlank());
        }
        assertTrue(settings.redactSecrets());
    }

    @Test
    void canDuplicateAProviderForAnotherModel() {
        AppSettings settings = AppSettings.defaults();
        ProviderProfile original = settings.selectedProviderProfile();

        ProviderProfile copy = settings.duplicateProviderProfile(original.id());
        copy.name("Local Llama");
        copy.config().model("llama3.1:8b");

        assertEquals(4, settings.providerProfiles().size());
        assertEquals(copy.id(), settings.selectedProviderId());
        assertEquals("llama3.1:8b", settings.selectedProviderProfile().config().model());
        assertTrue(settings.providerProfiles().stream()
                .anyMatch(profile -> profile.id().equals(original.id())));
    }

    @Test
    void doesNotRemoveTheLastProfile() {
        AppSettings settings = AppSettings.defaults();

        for (ProviderProfile profile : Set.copyOf(settings.providerProfiles())) {
            settings.removeProviderProfile(profile.id());
        }

        assertEquals(1, settings.providerProfiles().size());
        assertEquals(settings.providerProfiles().get(0).id(), settings.selectedProviderId());
    }
}
