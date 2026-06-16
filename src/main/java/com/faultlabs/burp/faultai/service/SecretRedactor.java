package com.faultlabs.burp.faultai.service;

import java.util.regex.Pattern;

public final class SecretRedactor {
    private static final Pattern SENSITIVE_HEADER = Pattern.compile(
            "(?im)^(Authorization|Proxy-Authorization|Cookie|Set-Cookie|X-Api-Key|Api-Key):\\s*.*$"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*"
    );
    private static final Pattern JWT = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\b"
    );
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\"(?:password|passwd|secret|token|api[_-]?key|access[_-]?token|refresh[_-]?token)\"\\s*:\\s*\")[^\"]*(\")"
    );
    private static final Pattern FORM_SECRET = Pattern.compile(
            "(?i)(\\b(?:password|passwd|secret|token|api[_-]?key|access[_-]?token|refresh[_-]?token)=)[^&\\s]*"
    );

    private SecretRedactor() {
    }

    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = SENSITIVE_HEADER.matcher(input)
                .replaceAll("$1: [REDACTED]");
        result = BEARER_TOKEN.matcher(result).replaceAll("Bearer [REDACTED]");
        result = JWT.matcher(result).replaceAll("[REDACTED_JWT]");
        result = JSON_SECRET.matcher(result).replaceAll("$1[REDACTED]$2");
        return FORM_SECRET.matcher(result).replaceAll("$1[REDACTED]");
    }
}
