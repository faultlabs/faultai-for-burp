package com.faultlabs.burp.faultai.model;

public record ChatMessage(Role role, String content) {
    public enum Role {
        USER,
        ASSISTANT
    }
}
