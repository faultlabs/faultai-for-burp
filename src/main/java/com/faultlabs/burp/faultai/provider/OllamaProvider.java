package com.faultlabs.burp.faultai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.faultlabs.burp.faultai.config.ProviderConfig;
import com.faultlabs.burp.faultai.model.ChatMessage;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class OllamaProvider extends HttpProviderSupport {
    @Override
    public String complete(
            ProviderConfig config,
            String systemPrompt,
            List<ChatMessage> messages
    ) throws Exception {
        validate(config);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("stream", false);

        JsonArray apiMessages = new JsonArray();
        apiMessages.add(message("system", systemPrompt));
        for (ChatMessage chatMessage : messages) {
            apiMessages.add(message(
                    chatMessage.role() == ChatMessage.Role.USER ? "user" : "assistant",
                    chatMessage.content()
            ));
        }
        body.add("messages", apiMessages);

        JsonObject options = new JsonObject();
        options.addProperty("temperature", config.temperature());
        options.addProperty("num_predict", config.maxOutputTokens());
        body.add("options", options);

        HttpRequest.Builder request = jsonRequest(
                endpoint(config.endpoint(), "/api/chat"),
                GSON.toJson(body)
        );
        addOptionalBearer(request, config.apiKey());

        JsonObject response = GSON.fromJson(send(request.build()).body(), JsonObject.class);
        if (!response.has("message")
                || !response.getAsJsonObject("message").has("content")) {
            throw new ProviderException("Ollama returned no assistant message.");
        }
        return response.getAsJsonObject("message").get("content").getAsString();
    }

    @Override
    public void testConnection(ProviderConfig config) throws Exception {
        if (config.endpoint().isBlank()) {
            throw new ProviderException("Ollama endpoint is required.");
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create(endpoint(config.endpoint(), "/api/tags"))
                )
                .timeout(Duration.ofSeconds(30))
                .GET();
        addOptionalBearer(request, config.apiKey());
        send(request.build());
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static void addOptionalBearer(HttpRequest.Builder builder, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey.trim());
        }
    }

    private static void validate(ProviderConfig config) throws ProviderException {
        if (config.endpoint().isBlank()) {
            throw new ProviderException("Ollama endpoint is required.");
        }
        if (config.model().isBlank()) {
            throw new ProviderException("Ollama model is required.");
        }
    }
}
