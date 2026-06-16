package com.faultlabs.burp.faultai.config;

public final class ProviderConfig {
    private String endpoint;
    private String model;
    private int maxOutputTokens;
    private double temperature;
    private transient String apiKey;

    public ProviderConfig() {
    }

    public ProviderConfig(String endpoint, String model, int maxOutputTokens, double temperature) {
        this.endpoint = endpoint;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    public ProviderConfig copy() {
        ProviderConfig copy = new ProviderConfig(endpoint, model, maxOutputTokens, temperature);
        copy.apiKey = apiKey;
        return copy;
    }

    public String endpoint() {
        return endpoint == null ? "" : endpoint.trim();
    }

    public void endpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String model() {
        return model == null ? "" : model.trim();
    }

    public void model(String model) {
        this.model = model;
    }

    public int maxOutputTokens() {
        return maxOutputTokens;
    }

    public void maxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public double temperature() {
        return temperature;
    }

    public void temperature(double temperature) {
        this.temperature = temperature;
    }

    public String apiKey() {
        return apiKey == null ? "" : apiKey;
    }

    public void apiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
