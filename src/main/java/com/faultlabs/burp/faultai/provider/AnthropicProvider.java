package com.faultlabs.burp.faultai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.faultlabs.burp.faultai.config.ProviderConfig;
import com.faultlabs.burp.faultai.model.ChatMessage;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class AnthropicProvider extends HttpProviderSupport {
    private static final String API_VERSION = "2023-06-01";

    @Override
    public String complete(
            ProviderConfig config,
            String systemPrompt,
            List<ChatMessage> messages
    ) throws Exception {
        validate(config);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("system", systemPrompt);
        body.addProperty("max_tokens", config.maxOutputTokens());
        body.addProperty("temperature", config.temperature());

        JsonArray apiMessages = new JsonArray();
        for (ChatMessage chatMessage : messages) {
            JsonObject message = new JsonObject();
            message.addProperty(
                    "role",
                    chatMessage.role() == ChatMessage.Role.USER ? "user" : "assistant"
            );
            message.addProperty("content", chatMessage.content());
            apiMessages.add(message);
        }
        body.add("messages", apiMessages);

        HttpRequest request = jsonRequest(
                endpoint(config.endpoint(), "/v1/messages"),
                GSON.toJson(body)
        )
                .header("x-api-key", config.apiKey().trim())
                .header("anthropic-version", API_VERSION)
                .build();

        JsonObject response = GSON.fromJson(send(request).body(), JsonObject.class);
        return extractText(response);
    }

    @Override
    public void testConnection(ProviderConfig config) throws Exception {
        validate(config);
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(endpoint(config.endpoint(), "/v1/models/" + config.model()))
                )
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", config.apiKey().trim())
                .header("anthropic-version", API_VERSION)
                .GET()
                .build();
        send(request);
    }

    static String extractText(JsonObject response) throws ProviderException {
        StringBuilder text = new StringBuilder();
        JsonArray content = response.has("content")
                ? response.getAsJsonArray("content")
                : new JsonArray();
        for (JsonElement item : content) {
            if (item.isJsonObject() && item.getAsJsonObject().has("text")) {
                text.append(item.getAsJsonObject().get("text").getAsString());
            }
        }
        if (text.isEmpty()) {
            throw new ProviderException("Anthropic returned no text output.");
        }
        return text.toString();
    }

    private static void validate(ProviderConfig config) throws ProviderException {
        if (config.endpoint().isBlank()) {
            throw new ProviderException("Anthropic endpoint is required.");
        }
        if (config.model().isBlank()) {
            throw new ProviderException("Anthropic model is required.");
        }
        if (config.apiKey().isBlank()) {
            throw new ProviderException(
                    "Anthropic API key is required. Configure it in Settings or ANTHROPIC_API_KEY."
            );
        }
    }
}
