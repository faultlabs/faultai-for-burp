package com.faultlabs.burp.faultai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRedactorTest {
    @Test
    void redactsHeadersTokensAndBodySecrets() {
        String input = """
                Authorization: Bearer top-secret
                Cookie: session=abc123
                X-Api-Key: key-123

                {"password":"hunter2","access_token":"token-value"}
                refresh_token=form-secret&safe=value
                """;

        String redacted = SecretRedactor.redact(input);

        assertFalse(redacted.contains("top-secret"));
        assertFalse(redacted.contains("abc123"));
        assertFalse(redacted.contains("key-123"));
        assertFalse(redacted.contains("hunter2"));
        assertFalse(redacted.contains("token-value"));
        assertFalse(redacted.contains("form-secret"));
        assertTrue(redacted.contains("[REDACTED]"));
        assertTrue(redacted.contains("safe=value"));
    }

    @Test
    void redactsJwtLikeValues() {
        String input = "token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature123";

        String redacted = SecretRedactor.redact(input);

        assertFalse(redacted.contains("eyJhbGci"));
        assertTrue(redacted.contains("[REDACTED]"));
    }
}
