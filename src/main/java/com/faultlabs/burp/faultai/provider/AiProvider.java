package com.faultlabs.burp.faultai.provider;

import com.faultlabs.burp.faultai.config.ProviderConfig;
import com.faultlabs.burp.faultai.model.ChatMessage;

import java.util.List;

public interface AiProvider {
    String complete(
            ProviderConfig config,
            String systemPrompt,
            List<ChatMessage> messages
    ) throws Exception;

    void testConnection(ProviderConfig config) throws Exception;
}
