package com.faultlabs.burp.faultai.ui;

import com.faultlabs.burp.faultai.config.AppSettings;
import com.faultlabs.burp.faultai.config.ProviderConfig;
import com.faultlabs.burp.faultai.config.ProviderProfile;
import com.faultlabs.burp.faultai.config.ProviderType;
import com.faultlabs.burp.faultai.config.SettingsStore;
import com.faultlabs.burp.faultai.service.AiService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

final class SettingsDialog extends JDialog {
    private final SettingsStore settingsStore;
    private final AiService aiService;
    private final Runnable onSave;
    private final AppSettings draft;

    private final JComboBox<ProviderProfile> profileSelector = new JComboBox<>();
    private final JTextField profileName = new JTextField(30);
    private final JComboBox<ProviderType> providerType =
            new JComboBox<>(ProviderType.values());
    private final JTextField endpoint = new JTextField(42);
    private final JTextField model = new JTextField(30);
    private final JPasswordField apiKey = new JPasswordField(30);
    private final JSpinner maxTokens = new JSpinner(
            new SpinnerNumberModel(4_096, 128, 131_072, 128)
    );
    private final JSpinner temperature = new JSpinner(
            new SpinnerNumberModel(0.2, 0.0, 2.0, 0.1)
    );
    private final JCheckBox redactSecrets =
            new JCheckBox("Redact common secrets before sending", true);
    private final JCheckBox includeResponse =
            new JCheckBox("Include HTTP responses", true);
    private final JSpinner contextLimit = new JSpinner(
            new SpinnerNumberModel(80_000, 4_000, 2_000_000, 4_000)
    );
    private final JTextArea systemPrompt = new JTextArea(8, 56);
    private final JLabel testStatus = new JLabel(" ");
    private String loadedProfileId;
    private boolean loadingProfiles;

    SettingsDialog(
            Window owner,
            SettingsStore settingsStore,
            AiService aiService,
            Runnable onSave
    ) {
        super(owner instanceof Frame ? (Frame) owner : null, "FaultAI settings", true);
        this.settingsStore = settingsStore;
        this.aiService = aiService;
        this.onSave = onSave;
        this.draft = settingsStore.get();

        buildUi();
        loadGeneralSettings();
        refreshProfileSelector(draft.selectedProviderId());
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Model profiles"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;

        int row = 0;
        addRow(form, constraints, row++, "Profile", profileSelector);
        addRow(form, constraints, row++, "Profile name", profileName);
        addRow(form, constraints, row++, "Provider type", providerType);
        addRow(form, constraints, row++, "Endpoint", endpoint);
        addRow(form, constraints, row++, "Model", model);
        addRow(form, constraints, row++, "API key", apiKey);
        addRow(form, constraints, row++, "Max output tokens", maxTokens);
        addRow(form, constraints, row++, "Temperature", temperature);

        JPanel profileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addProfile = new JButton("Add profile");
        addProfile.addActionListener(event -> addProfile());
        JButton duplicateProfile = new JButton("Duplicate");
        duplicateProfile.addActionListener(event -> duplicateProfile());
        JButton removeProfile = new JButton("Remove");
        removeProfile.addActionListener(event -> removeProfile());
        JButton testButton = new JButton("Test connection");
        testButton.addActionListener(event -> testConnection(testButton));
        profileButtons.add(addProfile);
        profileButtons.add(duplicateProfile);
        profileButtons.add(removeProfile);
        profileButtons.add(testButton);
        profileButtons.add(new JLabel("   "));
        profileButtons.add(testStatus);
        addRow(form, constraints, row, "", profileButtons);
        root.add(form, BorderLayout.NORTH);

        JPanel behavior = new JPanel(new GridBagLayout());
        behavior.setBorder(BorderFactory.createTitledBorder("Behavior and privacy"));
        GridBagConstraints behaviorConstraints = new GridBagConstraints();
        behaviorConstraints.insets = new Insets(5, 6, 5, 6);
        behaviorConstraints.anchor = GridBagConstraints.WEST;
        behaviorConstraints.fill = GridBagConstraints.HORIZONTAL;
        behaviorConstraints.weightx = 1;

        int behaviorRow = 0;
        addFullRow(behavior, behaviorConstraints, behaviorRow++, redactSecrets);
        addFullRow(behavior, behaviorConstraints, behaviorRow++, includeResponse);
        addRow(
                behavior,
                behaviorConstraints,
                behaviorRow++,
                "Maximum context characters",
                contextLimit
        );

        systemPrompt.setLineWrap(true);
        systemPrompt.setWrapStyleWord(true);
        JScrollPane systemPromptScroll = new JScrollPane(systemPrompt);
        addRow(
                behavior,
                behaviorConstraints,
                behaviorRow,
                "System prompt",
                systemPromptScroll
        );
        root.add(behavior, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> dispose());
        JButton save = new JButton("Save");
        save.addActionListener(event -> saveAndClose());
        buttons.add(new JLabel("Keys are kept in memory only. "));
        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);

        profileSelector.addActionListener(event -> switchProfile());
        setContentPane(root);
    }

    private void switchProfile() {
        if (loadingProfiles) {
            return;
        }
        ProviderProfile selected = selectedProfile();
        if (selected == null || selected.id().equals(loadedProfileId)) {
            return;
        }
        if (loadedProfileId != null) {
            saveProfileFields(loadedProfileId);
        }
        loadedProfileId = selected.id();
        loadProfile(selected);
        testStatus.setText(" ");
    }

    private void addProfile() {
        saveLoadedProfile();
        ProviderType type = (ProviderType) providerType.getSelectedItem();
        ProviderProfile added = draft.addProviderProfile(type == null ? ProviderType.OLLAMA : type);
        refreshProfileSelector(added.id());
        testStatus.setText(" ");
    }

    private void duplicateProfile() {
        ProviderProfile selected = selectedProfile();
        if (selected == null) {
            return;
        }
        saveLoadedProfile();
        ProviderProfile copy = draft.duplicateProviderProfile(selected.id());
        refreshProfileSelector(copy.id());
        testStatus.setText(" ");
    }

    private void removeProfile() {
        ProviderProfile selected = selectedProfile();
        if (selected == null) {
            return;
        }
        if (draft.providerProfiles().size() <= 1) {
            JOptionPane.showMessageDialog(
                    this,
                    "At least one model profile is required.",
                    "Cannot remove profile",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        draft.removeProviderProfile(selected.id());
        refreshProfileSelector(draft.selectedProviderId());
        testStatus.setText(" ");
    }

    private void refreshProfileSelector(String selectedId) {
        draft.normalize();
        loadingProfiles = true;
        profileSelector.removeAllItems();
        ProviderProfile selected = null;
        for (ProviderProfile profile : draft.providerProfiles()) {
            profileSelector.addItem(profile);
            if (profile.id().equals(selectedId)) {
                selected = profile;
            }
        }
        if (selected == null) {
            selected = draft.selectedProviderProfile();
        }
        profileSelector.setSelectedItem(selected);
        loadingProfiles = false;
        loadedProfileId = selected.id();
        loadProfile(selected);
    }

    private void loadGeneralSettings() {
        redactSecrets.setSelected(draft.redactSecrets());
        includeResponse.setSelected(draft.includeResponse());
        contextLimit.setValue(draft.maxContextCharacters());
        systemPrompt.setText(draft.systemPrompt());
        systemPrompt.setCaretPosition(0);
    }

    private void loadProfile(ProviderProfile profile) {
        ProviderConfig config = profile.config();
        profileName.setText(profile.name());
        providerType.setSelectedItem(profile.type());
        endpoint.setText(config.endpoint());
        model.setText(config.model());
        apiKey.setText(config.apiKey());
        maxTokens.setValue(config.maxOutputTokens());
        temperature.setValue(config.temperature());
    }

    private void saveLoadedProfile() {
        if (loadedProfileId != null) {
            saveProfileFields(loadedProfileId);
        }
    }

    private void saveProfileFields(String profileId) {
        ProviderProfile profile = draft.providerProfile(profileId);
        if (profile == null) {
            return;
        }

        String name = profileName.getText().trim();
        profile.name(name.isBlank() ? "Untitled profile" : name);
        ProviderType selectedType = (ProviderType) providerType.getSelectedItem();
        profile.type(selectedType == null ? ProviderType.OLLAMA : selectedType);

        ProviderConfig config = profile.config();
        config.endpoint(endpoint.getText().trim());
        config.model(model.getText().trim());
        config.apiKey(new String(apiKey.getPassword()));
        config.maxOutputTokens((Integer) maxTokens.getValue());
        config.temperature(((Number) temperature.getValue()).doubleValue());
        profile.normalize();
    }

    private void saveAndClose() {
        saveLoadedProfile();
        ProviderProfile selected = selectedProfile();
        if (selected != null) {
            draft.selectedProviderId(selected.id());
        }
        draft.redactSecrets(redactSecrets.isSelected());
        draft.includeResponse(includeResponse.isSelected());
        draft.maxContextCharacters((Integer) contextLimit.getValue());
        draft.systemPrompt(systemPrompt.getText().trim());
        settingsStore.save(draft);
        onSave.run();
        dispose();
    }

    private void testConnection(JButton button) {
        saveLoadedProfile();
        ProviderProfile selected = selectedProfile();
        if (selected == null) {
            return;
        }
        button.setEnabled(false);
        testStatus.setText("Testing...");
        aiService.testConnection(selected.type(), selected.config().copy())
                .whenComplete((unused, throwable) -> SwingUtilities.invokeLater(() -> {
                    button.setEnabled(true);
                    if (throwable == null) {
                        testStatus.setText("Connection successful");
                    } else {
                        testStatus.setText("Connection failed");
                        JOptionPane.showMessageDialog(
                                this,
                                rootMessage(throwable),
                                "Connection failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }));
    }

    private ProviderProfile selectedProfile() {
        return (ProviderProfile) profileSelector.getSelectedItem();
    }

    private static void addRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(component, constraints);
    }

    private static void addFullRow(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            java.awt.Component component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        panel.add(component, constraints);
        constraints.gridwidth = 1;
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
}
