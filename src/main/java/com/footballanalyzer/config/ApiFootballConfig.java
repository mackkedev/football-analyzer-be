package com.footballanalyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "api-football")
public class ApiFootballConfig {
    private String baseUrl;
    private String apiKey;
    private Integer rateLimitPerMinute;
    private Map<String, Integer> leagues;
    private Integer currentSeason;

    /**
     * Returns today's date adjusted to the season year.
     * E.g. if today is 2026-02-24 and season is 2024, returns 2025-02-24.
     */
    public LocalDate getSeasonAdjustedDate() {
        LocalDate today = LocalDate.now();
        int yearOffset = today.getYear() - (currentSeason + 1);
        return today.minusYears(yearOffset);
    }
}
