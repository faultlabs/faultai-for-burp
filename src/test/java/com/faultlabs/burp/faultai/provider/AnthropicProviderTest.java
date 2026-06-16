package com.faultlabs.burp.faultai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnthropicProviderTest {
    @Test
    void extractsAllTextBlocks() throws Exception {
        JsonObject response = JsonParser.parseString("""
                {
                  "content": [
                    {"type": "text", "text": "First "},
                    {"type": "text", "text": "answer"}
                  ]
                }
                """).getAsJsonObject();

        assertEquals("First answer", AnthropicProvider.extractText(response));
    }

    @Test
    void rejectsResponseWithoutText() {
        JsonObject response = JsonParser.parseString("{\"content\":[]}").getAsJsonObject();

        assertThrows(ProviderException.class, () -> AnthropicProvider.extractText(response));
    }
}
