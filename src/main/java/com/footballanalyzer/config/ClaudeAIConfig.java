package com.footballanalyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "claude")
public class ClaudeAIConfig {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer maxTokens;
}
