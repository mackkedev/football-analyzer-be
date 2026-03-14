package com.footballanalyzer.integration.footballdata;

import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class FootballDataClient {

    private final WebClient webClient;

    public FootballDataClient(@Qualifier("footballDataWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get matches for a specific matchday in a competition.
     * GET /v4/competitions/{code}/matches?matchday=N
     */
    public MatchesResponse getMatches(String competitionCode, int matchday) {
        log.info("Fetching matches for competition={}, matchday={}", competitionCode, matchday);
        rateLimitDelay();
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/competitions/{code}/matches")
                            .queryParam("matchday", matchday)
                            .build(competitionCode))
                    .retrieve()
                    .bodyToMono(MatchesResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error fetching matches for {}, matchday={}: {} - {}",
                    competitionCode, matchday, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch matches for competition {}: {}", competitionCode, e.getMessage());
            return null;
        }
    }

    /**
     * Get all matches for a competition (current season, all statuses).
     * Used for fixture sync — FixtureService filters out finished/live matches.
     * GET /v4/competitions/{code}/matches
     */
    public MatchesResponse getAllMatches(String competitionCode) {
        log.info("Fetching all matches for competition={}", competitionCode);
        rateLimitDelay();
        try {
            return webClient.get()
                    .uri("/competitions/{code}/matches", competitionCode)
                    .retrieve()
                    .bodyToMono(MatchesResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error fetching all matches for {}: {} - {}",
                    competitionCode, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch all matches for competition {}: {}", competitionCode, e.getMessage());
            return null;
        }
    }

    /**
     * Get matches for a competition filtered by status.
     * GET /v4/competitions/{code}/matches?status=FINISHED
     */
    public MatchesResponse getMatchesByStatus(String competitionCode, String status) {
        log.info("Fetching {} matches for competition={}", status, competitionCode);
        rateLimitDelay();
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/competitions/{code}/matches")
                            .queryParam("status", status)
                            .build(competitionCode))
                    .retrieve()
                    .bodyToMono(MatchesResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error fetching {} matches for {}: {} - {}",
                    status, competitionCode, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch {} matches for competition {}: {}", status, competitionCode, e.getMessage());
            return null;
        }
    }

    /**
     * Raw API call that returns the response body as a String — used for debugging only.
     * GET /v4/competitions/{code}/matches?matchday=N
     */
    public String getMatchesRaw(String competitionCode, int matchday) {
        rateLimitDelay();
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/competitions/{code}/matches")
                            .queryParam("matchday", matchday)
                            .build(competitionCode))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get a single match including head-to-head data.
     * GET /v4/matches/{id}
     */
    public SingleMatchResponse getMatch(int matchId) {
        log.info("Fetching match id={}", matchId);
        rateLimitDelay();
        try {
            return webClient.get()
                    .uri("/matches/{id}", matchId)
                    .retrieve()
                    .bodyToMono(SingleMatchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error fetching match {}: {} - {}",
                    matchId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch match {}: {}", matchId, e.getMessage());
            return null;
        }
    }

    /**
     * Get standings for a competition.
     * GET /v4/competitions/{code}/standings
     */
    public StandingsResponse getStandings(String competitionCode) {
        log.info("Fetching standings for competition={}", competitionCode);
        rateLimitDelay();
        try {
            return webClient.get()
                    .uri("/competitions/{code}/standings", competitionCode)
                    .retrieve()
                    .bodyToMono(StandingsResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error fetching standings for {}: {} - {}",
                    competitionCode, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch standings for competition {}: {}", competitionCode, e.getMessage());
            return null;
        }
    }

    /** 6500ms delay keeps us safely under the 10 req/min free-tier limit. */
    private void rateLimitDelay() {
        try {
            Thread.sleep(6500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
