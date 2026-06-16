package com.faultlabs.burp.faultai;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.faultlabs.burp.faultai.config.SettingsStore;
import com.faultlabs.burp.faultai.service.AiService;
import com.faultlabs.burp.faultai.ui.FaultAIContextMenuProvider;
import com.faultlabs.burp.faultai.ui.FaultAIPanel;

public final class FaultAIExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("FaultAI");

        SettingsStore settingsStore = new SettingsStore(api.persistence().preferences());
        AiService aiService = new AiService();
        FaultAIPanel panel = new FaultAIPanel(api, settingsStore, aiService);

        api.userInterface().applyThemeToComponent(panel);
        api.userInterface().registerSuiteTab("FaultAI", panel);
        api.userInterface().registerContextMenuItemsProvider(
                new FaultAIContextMenuProvider(panel)
        );
        api.extension().registerUnloadingHandler(aiService::close);
        api.logging().logToOutput(
                "FaultAI loaded. Configure Ollama, OpenAI/Codex, or Anthropic/Claude "
                        + "from the FaultAI tab."
        );
    }
}
