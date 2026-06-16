package com.faultlabs.burp.faultai.provider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

abstract class HttpProviderSupport implements AiProvider {
    protected static final Gson GSON = new Gson();
    protected final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    protected HttpResponse<String> send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ProviderException(errorMessage(response));
        }
        return response;
    }

    protected HttpRequest.Builder jsonRequest(String url, String json) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
    }

    protected static String endpoint(String base, String path) {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase + path;
    }

    private static String errorMessage(HttpResponse<String> response) {
        String detail = response.body();
        try {
            JsonObject object = GSON.fromJson(detail, JsonObject.class);
            if (object.has("error")) {
                if (object.get("error").isJsonObject()
                        && object.getAsJsonObject("error").has("message")) {
                    detail = object.getAsJsonObject("error").get("message").getAsString();
                } else {
                    detail = object.get("error").toString();
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the raw response body.
        }
        if (detail == null || detail.isBlank()) {
            detail = "No error detail returned";
        }
        if (detail.length() > 1_000) {
            detail = detail.substring(0, 1_000) + "...";
        }
        return "Provider returned HTTP " + response.statusCode() + ": " + detail;
    }
}
