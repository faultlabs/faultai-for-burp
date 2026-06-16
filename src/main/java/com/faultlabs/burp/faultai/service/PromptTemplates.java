package com.faultlabs.burp.faultai.service;

public final class PromptTemplates {
    public enum Action {
        CHAT("Send to chat"),
        ANALYZE("Analyze security"),
        EXPLAIN("Explain"),
        TEST_IDEAS("Suggest test cases");

        private final String label;

        Action(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private PromptTemplates() {
    }

    public static String prompt(Action action, String context) {
        return switch (action) {
            case CHAT -> """
                    Use the following HTTP exchange as context for my next questions.
                    Summarize the endpoint's purpose and highlight anything immediately notable.

                    %s
                    """.formatted(context);
            case ANALYZE -> """
                    Analyze this HTTP exchange for security weaknesses. For each plausible finding:
                    1. State the evidence in the traffic.
                    2. Explain why it may matter.
                    3. Give a minimal authorized validation procedure in Burp Repeater.
                    4. Include false-positive checks.
                    Rank findings by likely impact and confidence. Do not present hypotheses as confirmed.

                    %s
                    """.formatted(context);
            case EXPLAIN -> """
                    Explain this HTTP exchange to a security tester. Cover the request flow, important
                    headers and parameters, authentication/session behavior, encodings, and unusual
                    technologies. Call out security implications without claiming unsupported findings.

                    %s
                    """.formatted(context);
            case TEST_IDEAS -> """
                    Produce a focused manual test plan for this HTTP exchange. Include concrete mutations
                    suitable for Burp Repeater, expected signals, and stop conditions. Prioritize access
                    control, injection, request smuggling/desync indicators, server-side request behavior,
                    business logic, and session handling only where the traffic makes them relevant.

                    %s
                    """.formatted(context);
        };
    }
}
