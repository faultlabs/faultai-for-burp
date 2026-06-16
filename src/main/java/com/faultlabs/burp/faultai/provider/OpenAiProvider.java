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

public final class OpenAiProvider extends HttpProviderSupport {
    @Override
    public String complete(
            ProviderConfig config,
            String systemPrompt,
            List<ChatMessage> messages
    ) throws Exception {
        validate(config);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("instructions", systemPrompt);
        body.addProperty("max_output_tokens", config.maxOutputTokens());

        JsonArray input = new JsonArray();
        for (ChatMessage chatMessage : messages) {
            JsonObject message = new JsonObject();
            message.addProperty(
                    "role",
                    chatMessage.role() == ChatMessage.Role.USER ? "user" : "assistant"
            );
            message.addProperty("content", chatMessage.content());
            if (chatMessage.role() == ChatMessage.Role.ASSISTANT) {
                message.addProperty("phase", "final_answer");
            }
            input.add(message);
        }
        body.add("input", input);

        HttpRequest request = jsonRequest(
                endpoint(config.endpoint(), "/responses"),
                GSON.toJson(body)
        )
                .header("Authorization", "Bearer " + config.apiKey().trim())
                .build();

        JsonObject response = GSON.fromJson(send(request).body(), JsonObject.class);
        return extractText(response);
    }

    @Override
    public void testConnection(ProviderConfig config) throws Exception {
        validate(config);
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(endpoint(config.endpoint(), "/models/" + config.model()))
                )
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + config.apiKey().trim())
                .GET()
                .build();
        send(request);
    }

    static String extractText(JsonObject response) throws ProviderException {
        if (response.has("output_text") && response.get("output_text").isJsonPrimitive()) {
            return response.get("output_text").getAsString();
        }

        StringBuilder text = new StringBuilder();
        JsonArray output = response.has("output")
                ? response.getAsJsonArray("output")
                : new JsonArray();
        for (JsonElement outputItem : output) {
            if (!outputItem.isJsonObject() || !outputItem.getAsJsonObject().has("content")) {
                continue;
            }
            for (JsonElement contentItem : outputItem.getAsJsonObject().getAsJsonArray("content")) {
                if (!contentItem.isJsonObject()) {
                    continue;
                }
                JsonObject content = contentItem.getAsJsonObject();
                if (content.has("text")) {
                    text.append(content.get("text").getAsString());
                }
            }
        }
        if (text.isEmpty()) {
            throw new ProviderException("OpenAI returned no text output.");
        }
        return text.toString();
    }

    private static void validate(ProviderConfig config) throws ProviderException {
        if (config.endpoint().isBlank()) {
            throw new ProviderException("OpenAI endpoint is required.");
        }
        if (config.model().isBlank()) {
            throw new ProviderException("OpenAI model is required.");
        }
        if (config.apiKey().isBlank()) {
            throw new ProviderException(
                    "OpenAI API key is required. Configure it in Settings or OPENAI_API_KEY."
            );
        }
    }
}
