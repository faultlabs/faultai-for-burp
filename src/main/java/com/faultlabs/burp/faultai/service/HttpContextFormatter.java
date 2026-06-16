package com.faultlabs.burp.faultai.service;

import burp.api.montoya.http.message.HttpRequestResponse;
import com.faultlabs.burp.faultai.config.AppSettings;

public final class HttpContextFormatter {
    private HttpContextFormatter() {
    }

    public static String format(HttpRequestResponse exchange, AppSettings settings) {
        StringBuilder context = new StringBuilder();
        context.append("=== REQUEST ===\n");
        context.append(exchange.request());

        if (settings.includeResponse() && exchange.hasResponse() && exchange.response() != null) {
            context.append("\n\n=== RESPONSE ===\n");
            context.append(exchange.response());
        }

        String result = context.toString();
        if (settings.redactSecrets()) {
            result = SecretRedactor.redact(result);
        }

        int limit = settings.maxContextCharacters();
        if (result.length() > limit) {
            result = result.substring(0, limit)
                    + "\n\n[Context truncated at " + limit + " characters]";
        }
        return result;
    }
}
