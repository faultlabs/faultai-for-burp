package com.faultlabs.burp.faultai.ui;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.faultlabs.burp.faultai.service.PromptTemplates;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.Collections;
import java.util.List;

public final class FaultAIContextMenuProvider implements ContextMenuItemsProvider {
    private final FaultAIPanel panel;

    public FaultAIContextMenuProvider(FaultAIPanel panel) {
        this.panel = panel;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        HttpRequestResponse exchange = event.messageEditorRequestResponse()
                .map(messageEditor -> messageEditor.requestResponse())
                .orElseGet(() -> event.selectedRequestResponses().stream()
                        .findFirst()
                        .orElse(null));

        if (exchange == null) {
            return Collections.emptyList();
        }

        HttpRequestResponse retainedExchange = exchange.copyToTempFile();
        JMenu menu = new JMenu("FaultAI");
        addItem(menu, retainedExchange, PromptTemplates.Action.CHAT);
        addItem(menu, retainedExchange, PromptTemplates.Action.ANALYZE);
        addItem(menu, retainedExchange, PromptTemplates.Action.EXPLAIN);
        addItem(menu, retainedExchange, PromptTemplates.Action.TEST_IDEAS);
        return List.of(menu);
    }

    private void addItem(
            JMenu menu,
            HttpRequestResponse exchange,
            PromptTemplates.Action action
    ) {
        JMenuItem item = new JMenuItem(action.label());
        item.addActionListener(event -> panel.useExchange(exchange, action));
        menu.add(item);
    }
}
