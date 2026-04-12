package com.example.product_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
public class SearchProperties {
    private String endpoint;
    private String apiKey;
    private String indexName = "products";
    private String semanticConfigurationName = "products-semantic-config";
    private boolean semanticEnabled = false;
    private boolean fuzzyEnabled = true;
    private int top = 20;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getSemanticConfigurationName() {
        return semanticConfigurationName;
    }

    public void setSemanticConfigurationName(String semanticConfigurationName) {
        this.semanticConfigurationName = semanticConfigurationName;
    }

    public boolean isSemanticEnabled() {
        return semanticEnabled;
    }

    public void setSemanticEnabled(boolean semanticEnabled) {
        this.semanticEnabled = semanticEnabled;
    }

    public boolean isFuzzyEnabled() {
        return fuzzyEnabled;
    }

    public void setFuzzyEnabled(boolean fuzzyEnabled) {
        this.fuzzyEnabled = fuzzyEnabled;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }
}
