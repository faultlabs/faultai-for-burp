package com.faultlabs.burp.faultai.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import com.faultlabs.burp.faultai.config.AppSettings;
import com.faultlabs.burp.faultai.config.ProviderProfile;
import com.faultlabs.burp.faultai.config.SettingsStore;
import com.faultlabs.burp.faultai.model.ChatMessage;
import com.faultlabs.burp.faultai.service.AiService;
import com.faultlabs.burp.faultai.service.HttpContextFormatter;
import com.faultlabs.burp.faultai.service.PromptTemplates;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class FaultAIPanel extends JPanel {
    private static final String WELCOME = """
            FaultAI is ready.

            Use conversation tabs for separate investigations. Each tab keeps its own
            message history and model profile, so one tab can use Ollama while another
            uses Codex or Claude.

            Ask a question here, or right-click an HTTP request/response anywhere in
            Burp and choose FaultAI.
            """;

    private final MontoyaApi api;
    private final SettingsStore settingsStore;
    private final AiService aiService;
    private final List<Conversation> conversations = new ArrayList<>();
    private final JTabbedPane conversationTabs = new JTabbedPane();
    private final JComboBox<ProviderProfile> providerSelector = new JComboBox<>();
    private final JEditorPane transcript = new JEditorPane();
    private final JTextArea promptInput = new JTextArea(5, 80);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton sendButton = new JButton("Send");
    private final JButton stopButton = new JButton("Stop");
    private final JButton clearButton = new JButton("Clear");
    private final JButton closeTabButton = new JButton("Close tab");
    private int nextConversationNumber = 1;
    private boolean loadingProfiles;
    private boolean loadingConversationTabs;

    public FaultAIPanel(
            MontoyaApi api,
            SettingsStore settingsStore,
            AiService aiService
    ) {
        super(new BorderLayout(0, 0));
        this.api = api;
        this.settingsStore = settingsStore;
        this.aiService = aiService;

        buildUi();
        applySettings();
        renderTranscript();
    }

    public void useExchange(HttpRequestResponse exchange, PromptTemplates.Action action) {
        AppSettings settings = settingsStore.get();
        String context = HttpContextFormatter.format(exchange, settings);
        String prompt = PromptTemplates.prompt(action, context);
        SwingUtilities.invokeLater(() -> submit(prompt));
    }

    private void buildUi() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);

        transcript.setEditable(false);
        transcript.setContentType("text/html");
        transcript.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        transcript.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        JScrollPane transcriptScroll = new JScrollPane(transcript);
        transcriptScroll.setBorder(BorderFactory.createTitledBorder("Conversation"));

        JPanel composer = buildComposer();
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                transcriptScroll,
                composer
        );
        splitPane.setResizeWeight(0.78);
        splitPane.setDividerLocation(520);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.add(buildToolbar(), BorderLayout.NORTH);

        conversationTabs.addChangeListener(event -> {
            if (loadingConversationTabs) {
                return;
            }
            syncProviderSelectorToCurrentConversation();
            renderTranscript();
            updateCurrentBusyState();
            updateCurrentStatus();
        });
        conversationTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                showConversationTabMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                showConversationTabMenu(event);
            }
        });
        header.add(conversationTabs, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("Model profile:"));
        providerSelector.addActionListener(event -> {
            if (loadingProfiles) {
                return;
            }
            ProviderProfile selected = (ProviderProfile) providerSelector.getSelectedItem();
            Conversation conversation = currentConversation();
            if (selected == null || conversation == null) {
                return;
            }
            conversation.selectedProviderId = selected.id();

            AppSettings settings = settingsStore.get();
            settings.selectedProviderId(selected.id());
            settingsStore.save(settings);

            updateConversationTab(conversation);
            updateStatus("Using " + selected.name() + " / " + selected.config().model());
        });
        toolbar.add(providerSelector);

        JButton newTabButton = new JButton("New tab");
        newTabButton.addActionListener(event -> addConversationTab());
        toolbar.add(newTabButton);

        clearButton.addActionListener(event -> clearConversation());
        toolbar.add(clearButton);

        closeTabButton.addActionListener(event -> closeCurrentConversation());
        toolbar.add(closeTabButton);

        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(event -> openSettings());
        toolbar.add(settingsButton);

        toolbar.add(Box.createHorizontalStrut(12));
        statusLabel.setBorder(new EmptyBorder(0, 6, 0, 0));
        toolbar.add(statusLabel);
        return toolbar;
    }

    private JPanel buildComposer() {
        JPanel composer = new JPanel(new BorderLayout(8, 8));
        composer.setBorder(BorderFactory.createTitledBorder("Prompt"));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        addQuickAction(quickActions, "Analyze", """
                Analyze the current conversation context for plausible security weaknesses.
                Rank them by impact and confidence, cite evidence, and give validation steps.
                """);
        addQuickAction(quickActions, "Explain", """
                Explain the current HTTP context, including important headers, parameters,
                authentication behavior, encodings, and security implications.
                """);
        addQuickAction(quickActions, "Test ideas", """
                Suggest focused, authorized Burp Repeater tests for the current context.
                Include mutations, expected signals, and false-positive checks.
                """);
        composer.add(quickActions, BorderLayout.NORTH);

        promptInput.setLineWrap(true);
        promptInput.setWrapStyleWord(true);
        promptInput.setTabSize(2);
        promptInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER
                        && (event.isControlDown() || event.isMetaDown())) {
                    event.consume();
                    submitFromInput();
                }
            }
        });
        JScrollPane promptScroll = new JScrollPane(promptInput);
        promptScroll.setPreferredSize(new Dimension(800, 130));
        composer.add(promptScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        sendButton.addActionListener(event -> submitFromInput());
        stopButton.addActionListener(event -> cancelActiveRequest());
        stopButton.setEnabled(false);
        sendButton.setAlignmentX(CENTER_ALIGNMENT);
        stopButton.setAlignmentX(CENTER_ALIGNMENT);
        buttons.add(sendButton);
        buttons.add(Box.createVerticalStrut(6));
        buttons.add(stopButton);
        composer.add(buttons, BorderLayout.EAST);

        JLabel hint = new JLabel("Ctrl/Cmd+Enter to send. API keys are memory-only.");
        hint.setBorder(new EmptyBorder(0, 4, 2, 0));
        composer.add(hint, BorderLayout.SOUTH);
        return composer;
    }

    private void addQuickAction(JPanel panel, String label, String prompt) {
        JButton button = new JButton(label);
        button.addActionListener(event -> {
            promptInput.setText(prompt.strip());
            promptInput.requestFocusInWindow();
        });
        panel.add(button);
    }

    private void submitFromInput() {
        String prompt = promptInput.getText().trim();
        if (prompt.isEmpty()) {
            return;
        }
        promptInput.setText("");
        submit(prompt);
    }

    private void submit(String prompt) {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            conversation = addConversationTab();
        }
        if (conversation.isBusy()) {
            updateStatus("Wait for the active request in this tab or stop it first.");
            return;
        }

        AppSettings settings = settingsStore.get();
        ProviderProfile profile = settings.providerProfile(conversation.selectedProviderId);
        if (profile == null) {
            profile = settings.selectedProviderProfile();
            conversation.selectedProviderId = profile.id();
            syncProviderSelectorToCurrentConversation();
        }
        settings.selectedProviderId(profile.id());

        conversation.messages.add(new ChatMessage(ChatMessage.Role.USER, prompt));
        renderTranscript();
        updateConversationTab(conversation);
        updateCurrentBusyState();
        updateStatus("Thinking with " + profile.name() + " / " + profile.config().model() + "...");

        List<ChatMessage> requestMessages = List.copyOf(conversation.messages);
        Conversation requestConversation = conversation;
        CompletableFuture<String> request = aiService.complete(settings, requestMessages);
        requestConversation.activeRequest = request;
        request.whenComplete((answer, throwable) -> SwingUtilities.invokeLater(() -> {
            if (request.isCancelled()) {
                if (requestConversation.activeRequest == request) {
                    requestConversation.activeRequest = null;
                }
                updateConversationTab(requestConversation);
                if (requestConversation == currentConversation()) {
                    updateCurrentBusyState();
                    updateStatus("Request stopped");
                }
                return;
            }

            if (requestConversation.activeRequest == request) {
                requestConversation.activeRequest = null;
            }
            if (throwable != null) {
                String message = rootMessage(throwable);
                requestConversation.messages.add(new ChatMessage(
                        ChatMessage.Role.ASSISTANT,
                        "Request failed: " + message
                ));
                api.logging().logToError("FaultAI request failed: " + message);
                if (requestConversation == currentConversation()) {
                    updateStatus("Request failed");
                }
            } else {
                requestConversation.messages.add(new ChatMessage(ChatMessage.Role.ASSISTANT, answer));
                if (requestConversation == currentConversation()) {
                    updateStatus("Ready");
                }
            }

            updateConversationTab(requestConversation);
            if (requestConversation == currentConversation()) {
                renderTranscript();
                updateCurrentBusyState();
            }
        }));
    }

    private void cancelActiveRequest() {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            return;
        }
        CompletableFuture<String> request = conversation.activeRequest;
        if (request != null && !request.isDone()) {
            request.cancel(true);
        }
        conversation.activeRequest = null;
        updateConversationTab(conversation);
        updateCurrentBusyState();
        updateStatus("Request stopped");
    }

    private void clearConversation() {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            return;
        }
        cancelActiveRequest();
        conversation.messages.clear();
        promptInput.setText("");
        renderTranscript();
        updateConversationTab(conversation);
        updateStatus(conversation.title + " cleared");
    }

    private void renameCurrentConversation() {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            return;
        }

        String newTitle = JOptionPane.showInputDialog(
                this,
                "Tab name:",
                conversation.title
        );
        if (newTitle == null) {
            return;
        }

        newTitle = newTitle.trim();
        if (newTitle.isBlank()) {
            return;
        }

        conversation.title = newTitle;
        updateConversationTab(conversation);
        updateStatus("Renamed tab to " + conversation.title);
    }

    private Conversation addConversationTab() {
        AppSettings settings = settingsStore.get();
        Conversation conversation = new Conversation(
                nextConversationNumber++,
                settings.selectedProviderId()
        );
        conversations.add(conversation);

        loadingConversationTabs = true;
        conversationTabs.addTab(conversation.title, new JPanel());
        conversationTabs.setSelectedIndex(conversations.size() - 1);
        loadingConversationTabs = false;

        syncProviderSelectorToCurrentConversation();
        renderTranscript();
        updateConversationTab(conversation);
        updateCurrentBusyState();
        updateCurrentStatus();
        return conversation;
    }

    private void closeCurrentConversation() {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            return;
        }
        if (conversations.size() == 1) {
            clearConversation();
            return;
        }

        int index = conversations.indexOf(conversation);
        cancelActiveRequest();
        conversations.remove(index);

        loadingConversationTabs = true;
        conversationTabs.removeTabAt(index);
        conversationTabs.setSelectedIndex(Math.max(0, index - 1));
        loadingConversationTabs = false;

        syncProviderSelectorToCurrentConversation();
        renderTranscript();
        updateCurrentBusyState();
        updateCurrentStatus();
    }

    private void showConversationTabMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }

        int tabIndex = conversationTabs.indexAtLocation(event.getX(), event.getY());
        if (tabIndex < 0 || tabIndex >= conversations.size()) {
            return;
        }
        conversationTabs.setSelectedIndex(tabIndex);

        JPopupMenu menu = new JPopupMenu();

        JMenuItem rename = new JMenuItem("Rename tab");
        rename.addActionListener(action -> renameCurrentConversation());
        menu.add(rename);

        JMenuItem clear = new JMenuItem("Clear conversation");
        clear.addActionListener(action -> clearConversation());
        menu.add(clear);

        menu.addSeparator();

        JMenuItem newTab = new JMenuItem("New tab");
        newTab.addActionListener(action -> addConversationTab());
        menu.add(newTab);

        JMenuItem close = new JMenuItem("Close tab");
        close.addActionListener(action -> closeCurrentConversation());
        menu.add(close);

        menu.show(conversationTabs, event.getX(), event.getY());
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(
                SwingUtilities.getWindowAncestor(this),
                settingsStore,
                aiService,
                this::applySettings
        );
        dialog.setVisible(true);
    }

    private void applySettings() {
        AppSettings settings = settingsStore.get();

        loadingProfiles = true;
        providerSelector.removeAllItems();
        for (ProviderProfile profile : settings.providerProfiles()) {
            providerSelector.addItem(profile);
        }
        loadingProfiles = false;

        if (conversations.isEmpty()) {
            addConversationTab();
        }

        for (Conversation conversation : conversations) {
            if (settings.providerProfile(conversation.selectedProviderId) == null) {
                conversation.selectedProviderId = settings.selectedProviderId();
            }
            updateConversationTab(conversation);
        }

        syncProviderSelectorToCurrentConversation();
        renderTranscript();
        updateCurrentBusyState();
        updateCurrentStatus();
    }

    private Conversation currentConversation() {
        int index = conversationTabs.getSelectedIndex();
        if (index < 0 || index >= conversations.size()) {
            return conversations.isEmpty() ? null : conversations.get(0);
        }
        return conversations.get(index);
    }

    private void syncProviderSelectorToCurrentConversation() {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            return;
        }
        ProviderProfile selected = comboProfileById(conversation.selectedProviderId);
        if (selected == null && providerSelector.getItemCount() > 0) {
            selected = providerSelector.getItemAt(0);
            conversation.selectedProviderId = selected.id();
        }

        loadingProfiles = true;
        providerSelector.setSelectedItem(selected);
        loadingProfiles = false;
    }

    private ProviderProfile comboProfileById(String id) {
        for (int index = 0; index < providerSelector.getItemCount(); index++) {
            ProviderProfile profile = providerSelector.getItemAt(index);
            if (profile.id().equals(id)) {
                return profile;
            }
        }
        return null;
    }

    private void updateConversationTab(Conversation conversation) {
        int index = conversations.indexOf(conversation);
        if (index < 0) {
            return;
        }
        String suffix = conversation.isBusy() ? " *" : "";
        conversationTabs.setTitleAt(index, conversation.title + suffix);

        ProviderProfile profile = comboProfileById(conversation.selectedProviderId);
        if (profile != null) {
            conversationTabs.setToolTipTextAt(
                    index,
                    profile.name() + " / " + profile.config().model()
            );
        }
    }

    private void updateCurrentBusyState() {
        Conversation conversation = currentConversation();
        boolean busy = conversation != null && conversation.isBusy();
        sendButton.setEnabled(!busy);
        stopButton.setEnabled(busy);
        clearButton.setEnabled(conversation != null);
        closeTabButton.setEnabled(conversation != null);
        providerSelector.setEnabled(!busy);
    }

    private void updateCurrentStatus() {
        Conversation conversation = currentConversation();
        if (conversation == null) {
            updateStatus("Ready");
            return;
        }
        ProviderProfile profile = comboProfileById(conversation.selectedProviderId);
        if (conversation.isBusy()) {
            String model = profile == null ? "selected model" : profile.name() + " / " + profile.config().model();
            updateStatus("Thinking with " + model + "...");
        } else if (profile == null) {
            updateStatus("Choose a model profile");
        } else {
            updateStatus("Using " + profile.name() + " / " + profile.config().model());
        }
    }

    private void updateStatus(String status) {
        statusLabel.setText(status);
    }

    private void renderTranscript() {
        Conversation conversation = currentConversation();
        List<ChatMessage> messages = conversation == null
                ? List.of()
                : conversation.messages;

        StringBuilder html = new StringBuilder("""
                <html><head><style>
                body { font-family: sans-serif; margin: 12px; }
                .welcome { color: #777; padding: 10px; }
                .message { margin: 8px 0 14px 0; padding: 10px; border-radius: 7px; }
                .user { background: #e7f0ff; color: #172033; }
                .assistant { background: #f0f0f0; color: #222; }
                .role { font-weight: bold; margin-bottom: 6px; }
                pre { white-space: pre-wrap; font-family: monospace; margin: 5px 0 0 0; }
                </style></head><body>
                """);

        if (messages.isEmpty()) {
            html.append("<div class='welcome'><pre>")
                    .append(escapeHtml(WELCOME))
                    .append("</pre></div>");
        } else {
            for (ChatMessage message : messages) {
                String cssClass = message.role() == ChatMessage.Role.USER
                        ? "user"
                        : "assistant";
                String role = message.role() == ChatMessage.Role.USER ? "You" : "FaultAI";
                html.append("<div class='message ")
                        .append(cssClass)
                        .append("'><div class='role'>")
                        .append(role)
                        .append("</div><pre>")
                        .append(escapeHtml(message.content()))
                        .append("</pre></div>");
            }
        }
        html.append("</body></html>");
        transcript.setText(html.toString());
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }

    private static final class Conversation {
        private String title;
        private final List<ChatMessage> messages = new ArrayList<>();
        private String selectedProviderId;
        private CompletableFuture<String> activeRequest;

        private Conversation(int number, String selectedProviderId) {
            this.title = "Chat " + number;
            this.selectedProviderId = selectedProviderId;
        }

        private boolean isBusy() {
            return activeRequest != null && !activeRequest.isDone();
        }
    }
}
