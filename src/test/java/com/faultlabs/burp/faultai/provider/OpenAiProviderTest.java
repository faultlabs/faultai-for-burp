package com.faultlabs.burp.faultai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiProviderTest {
    @Test
    void extractsTextFromResponsesOutput() throws Exception {
        JsonObject response = JsonParser.parseString("""
                {
                  "output": [{
                    "type": "message",
                    "content": [
                      {"type": "output_text", "text": "First "},
                      {"type": "output_text", "text": "answer"}
                    ]
                  }]
                }
                """).getAsJsonObject();

        assertEquals("First answer", OpenAiProvider.extractText(response));
    }

    @Test
    void rejectsResponseWithoutText() {
        JsonObject response = JsonParser.parseString("{\"output\":[]}").getAsJsonObject();

        assertThrows(ProviderException.class, () -> OpenAiProvider.extractText(response));
    }
}
