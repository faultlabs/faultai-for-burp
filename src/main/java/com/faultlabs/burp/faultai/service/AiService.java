package com.faultlabs.burp.faultai.service;

import com.faultlabs.burp.faultai.config.AppSettings;
import com.faultlabs.burp.faultai.config.ProviderConfig;
import com.faultlabs.burp.faultai.config.ProviderProfile;
import com.faultlabs.burp.faultai.config.ProviderType;
import com.faultlabs.burp.faultai.model.ChatMessage;
import com.faultlabs.burp.faultai.provider.AiProvider;
import com.faultlabs.burp.faultai.provider.ProviderFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AiService implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "faultai-provider");
        thread.setDaemon(true);
        return thread;
    });

    public CompletableFuture<String> complete(
            AppSettings settings,
            List<ChatMessage> messages
    ) {
        return CompletableFuture.supplyAsync(() -> {
            ProviderProfile profile = settings.selectedProviderProfile();
            ProviderConfig config = profile.config();
            AiProvider provider = ProviderFactory.create(profile.type());
            try {
                return provider.complete(config, settings.systemPrompt(), messages);
            } catch (Exception exception) {
                throw new AiRequestException(exception);
            }
        }, executor);
    }

    public CompletableFuture<Void> testConnection(
            ProviderType type,
            ProviderConfig config
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                ProviderFactory.create(type).testConnection(config);
            } catch (Exception exception) {
                throw new AiRequestException(exception);
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public static final class AiRequestException extends RuntimeException {
        public AiRequestException(Throwable cause) {
            super(cause);
        }
    }
}
