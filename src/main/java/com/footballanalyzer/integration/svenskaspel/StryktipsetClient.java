package com.footballanalyzer.integration.svenskaspel;

import com.footballanalyzer.integration.svenskaspel.dto.StryktipsetApiDtos.DrawsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class StryktipsetClient {

    private final WebClient webClient;

    public StryktipsetClient(@Qualifier("svenskaSpelWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetch current Stryktipset draws from Svenska Spel API.
     * Endpoint: GET /draw/1/stryktipset/draws
     */
    public DrawsResponse fetchDraws() {
        log.info("Fetching Stryktipset draws from Svenska Spel API");

        try {
            DrawsResponse response = webClient.get()
                    .uri("/draw/1/stryktipset/draws")
                    .retrieve()
                    .bodyToMono(DrawsResponse.class)
                    .block();

            if (response != null && response.getDraws() != null) {
                log.info("Fetched {} Stryktipset draws", response.getDraws().size());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch Stryktipset draws from Svenska Spel", e);
            return null;
        }
    }
}
