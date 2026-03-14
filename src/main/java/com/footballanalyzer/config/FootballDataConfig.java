package com.footballanalyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "football-data")
public class FootballDataConfig {
    private String baseUrl;
    private String apiKey;
    private Integer currentSeason;
    private Integer currentMatchday;
    private Map<String, String> competitions; // name → code (PL, SA, BL1, PD, ELC)
}
