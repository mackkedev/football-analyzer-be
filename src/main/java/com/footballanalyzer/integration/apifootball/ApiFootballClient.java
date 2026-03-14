package com.footballanalyzer.integration.apifootball;

import com.footballanalyzer.config.ApiFootballConfig;
import com.footballanalyzer.integration.apifootball.dto.ApiFootballDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
public class ApiFootballClient {

    private final WebClient webClient;
    private final ApiFootballConfig config;

    public ApiFootballClient(@Qualifier("apiFootballWebClient") WebClient webClient, ApiFootballConfig config) {
        this.webClient = webClient;
        this.config = config;
    }

    /**
     * Get fixtures for a league and round
     */
    public List<FixtureData> getFixtures(int leagueId, int season, String round) {
        log.info("Fetching fixtures for league={}, season={}, round={}", leagueId, season, round);

        ApiResponse<FixtureData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .queryParam("round", round)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<FixtureData>>() {})
                .block();

        return response != null ? response.getResponse() : List.of();
    }

    /**
     * Get fixtures for a league by date range
     */
    public List<FixtureData> getFixturesByDate(int leagueId, int season, String from, String to) {
        log.info("Fetching fixtures for league={}, from={}, to={}", leagueId, from, to);

        ApiResponse<FixtureData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<FixtureData>>() {})
                .block();

        return response != null ? response.getResponse() : List.of();
    }

    /**
     * Get fixture statistics (corners, cards, shots, etc.)
     */
    public List<FixtureStatisticsData> getFixtureStatistics(int fixtureId) {
        log.info("Fetching statistics for fixture={}", fixtureId);

        ApiResponse<FixtureStatisticsData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/statistics")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<FixtureStatisticsData>>() {})
                .block();

        return response != null ? response.getResponse() : List.of();
    }

    /**
     * Get fixture events (goals with minutes, cards, etc.)
     */
    public List<EventData> getFixtureEvents(int fixtureId) {
        log.info("Fetching events for fixture={}", fixtureId);

        ApiResponse<EventData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/events")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<EventData>>() {})
                .block();

        return response != null ? response.getResponse() : List.of();
    }

    /**
     * Get head-to-head fixtures between two teams
     */
    public List<FixtureData> getHeadToHead(int teamId1, int teamId2, int last) {
        log.info("Fetching H2H for teams {} vs {}", teamId1, teamId2);

        ApiResponse<FixtureData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/headtohead")
                        .queryParam("h2h", teamId1 + "-" + teamId2)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<FixtureData>>() {})
                .block();

        // Free plan doesn't support 'last' param, so limit in-memory
        List<FixtureData> all = response != null ? response.getResponse() : List.of();
        return all.size() > last ? all.subList(all.size() - last, all.size()) : all;
    }

    /**
     * Get current standings for a league
     */
    public List<StandingsData> getStandings(int leagueId, int season) {
        log.info("Fetching standings for league={}, season={}", leagueId, season);

        ApiResponse<StandingsData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/standings")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<StandingsData>>() {})
                .block();

        return response != null ? response.getResponse() : List.of();
    }

    /**
     * Get next round/upcoming fixtures for a league
     */
    public List<FixtureData> getNextFixtures(int leagueId, int season, int next) {
        log.info("Fetching next {} fixtures for league={} season {}", next, leagueId, season);

        ApiResponse<FixtureData> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .queryParam("next", next)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<FixtureData>>() {})
                .block();

        return response != null ? response.getResponse() : List.of();
    }
}
