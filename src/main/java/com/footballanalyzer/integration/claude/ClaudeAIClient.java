package com.footballanalyzer.integration.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballanalyzer.config.ClaudeAIConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeAIClient implements AIClient {

    private final WebClient webClient;
    private final ClaudeAIConfig config;
    private final ObjectMapper objectMapper;

    public ClaudeAIClient(@Qualifier("claudeWebClient") WebClient webClient, ClaudeAIConfig config, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Send a match analysis request to Claude and get structured JSON back.
     */
    public AnalysisResponse analyzeMatch(String matchContext) {
        log.info("Sending match analysis request to Claude (model={})", config.getModel());

        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "max_tokens", config.getMaxTokens(),
                "system", SYSTEM_PROMPT,
                "messages", List.of(
                        Map.of("role", "user", "content", matchContext + "\n\n" + RESPONSE_FORMAT_INSTRUCTIONS)
                )
        );

        try {
            ClaudeResponse response = webClient.post()
                    .uri("/messages")
                    .bodyValue(requestBody)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Claude API error ({}): {}", clientResponse.statusCode(), body))
                                    .then(reactor.core.publisher.Mono.empty());
                        }
                        return clientResponse.bodyToMono(ClaudeResponse.class);
                    })
                    .block();

            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                String rawText = response.getContent().get(0).getText();
                log.debug("Claude raw response: {}", rawText);

                // Extract JSON from response (Claude might wrap it in ```json blocks)
                String jsonStr = extractJson(rawText);
                AnalysisResponse analysisResponse = objectMapper.readValue(jsonStr, AnalysisResponse.class);
                analysisResponse.setRawResponse(rawText);
                analysisResponse.setModel(config.getModel());
                return analysisResponse;
            }
        } catch (Exception e) {
            log.error("Failed to get analysis from Claude", e);
        }
        return null;
    }

    /**
     * Analyze all 13 Stryktipset matches in a single call, returning 1/X/2 predictions.
     */
    public StryktipsetPredictionsResponse analyzeStryktipset(String matchContext) {
        log.info("Sending Stryktipset analysis request to Claude (model={})", config.getModel());

        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "max_tokens", 4000,
                "system", STRYKTIPSET_SYSTEM_PROMPT,
                "messages", List.of(
                        Map.of("role", "user", "content", matchContext + "\n\n" + STRYKTIPSET_RESPONSE_FORMAT)
                )
        );

        try {
            ClaudeResponse response = webClient.post()
                    .uri("/messages")
                    .bodyValue(requestBody)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Claude API error ({}): {}", clientResponse.statusCode(), body))
                                    .then(reactor.core.publisher.Mono.empty());
                        }
                        return clientResponse.bodyToMono(ClaudeResponse.class);
                    })
                    .block();

            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                String rawText = response.getContent().get(0).getText();
                log.debug("Claude Stryktipset raw response: {}", rawText);

                String jsonStr = extractJson(rawText);
                return objectMapper.readValue(jsonStr, StryktipsetPredictionsResponse.class);
            }
        } catch (Exception e) {
            log.error("Failed to get Stryktipset analysis from Claude", e);
        }
        return null;
    }

    @Override
    public StryktipsetCouponResponse generateStryktipsetCoupon(String matchContext) {
        log.warn("generateStryktipsetCoupon not implemented for ClaudeAIClient — use GeminiAIClient");
        return null;
    }

    @Override
    public BetBuilderResponse analyzeBetBuilder(String matchContext) {
        log.info("Sending bet builder analysis request to Claude (model={})", config.getModel());

        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "max_tokens", 6000,
                "system", BET_BUILDER_SYSTEM_PROMPT,
                "messages", List.of(
                        Map.of("role", "user", "content", matchContext + "\n\n" + BET_BUILDER_RESPONSE_FORMAT)
                )
        );

        try {
            ClaudeResponse response = webClient.post()
                    .uri("/messages")
                    .bodyValue(requestBody)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Claude API error ({}): {}", clientResponse.statusCode(), body))
                                    .then(reactor.core.publisher.Mono.empty());
                        }
                        return clientResponse.bodyToMono(ClaudeResponse.class);
                    })
                    .block();

            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                String rawText = response.getContent().get(0).getText();
                log.debug("Claude bet builder raw response: {}", rawText);

                String jsonStr = extractJson(rawText);
                BetBuilderResponse betResponse = objectMapper.readValue(jsonStr, BetBuilderResponse.class);
                betResponse.setModel(config.getModel());
                return betResponse;
            }
        } catch (Exception e) {
            log.error("Failed to get bet builder analysis from Claude", e);
        }
        return null;
    }

    private String extractJson(String text) {
        // Remove ```json ... ``` wrapper if present
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    // ==========================================
    // Claude API Response DTOs
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClaudeResponse {
        private String id;
        private String type;
        private String role;
        private List<ContentBlock> content;
        private String model;
        @JsonProperty("stop_reason")
        private String stopReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentBlock {
        private String type;
        private String text;
    }

    // ==========================================
    // Structured Analysis Response
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysisResponse {
        @JsonProperty("match_result")
        private PredictionField matchResult;
        private BttsPrediction btts;
        @JsonProperty("more_goals_2nd_half")
        private BooleanPrediction moreGoals2ndHalf;
        @JsonProperty("most_shots_on_goal")
        private TeamPrediction mostShotsOnGoal;
        @JsonProperty("most_yellow_cards")
        private TeamPrediction mostYellowCards;
        @JsonProperty("most_corners")
        private TeamPrediction mostCorners;
        @JsonProperty("data_basis")
        private String dataBasis;

        // Metadata (set after parsing)
        private transient String rawResponse;
        private transient String model;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PredictionField {
        private String prediction; // "1", "X", "2", "1,X", "X,2", "1,2"
        private Double confidence;
        private String reasoning;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BttsPrediction {
        private Boolean prediction;
        private Double confidence;
        private String reasoning;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BooleanPrediction {
        private Boolean prediction;
        private Double confidence;
        private String reasoning;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamPrediction {
        private String prediction; // "HOME" or "AWAY"
        private String reasoning;
    }

    // ==========================================
    // Stryktipset DTOs
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StryktipsetPredictionsResponse {
        private List<StryktipsetPrediction> predictions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StryktipsetPrediction {
        @JsonProperty("event_number")
        private Integer eventNumber;
        private String prediction; // "1", "X", "2"
        private Double confidence;
        private String reasoning;
    }

    // ==========================================
    // Bet Builder DTOs
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BetBuilderResponse {
        @JsonProperty("selected_matches")
        private List<BetBuilderMatchPick> selectedMatches;
        @JsonProperty("overall_reasoning")
        private String overallReasoning;
        private transient String model;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BetBuilderMatchPick {
        @JsonProperty("match_id")
        private Long matchId;
        @JsonProperty("bet_options")
        private List<BetBuilderOptionAI> betOptions;
        @JsonProperty("recommended_option")
        private Integer recommendedOption;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BetBuilderOptionAI {
        private String description;
        private List<String> selections;
        @JsonProperty("estimated_odds")
        private Double estimatedOdds;
        private Double confidence;
        private String reasoning;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StryktipsetCouponResponse {
        private List<StryktipsetCouponPick> picks;
        @JsonProperty("total_rows")
        private Integer totalRows;
        private String strategy;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StryktipsetCouponPick {
        @JsonProperty("event_number")
        private Integer eventNumber;
        private List<String> signs; // e.g. ["1"], ["1","X"], ["X","2"], ["1","2"]
        @JsonProperty("value_rating")
        private String valueRating; // "HIGH", "MEDIUM", "LOW"
        private String reasoning;
    }

    // ==========================================
    // Prompts
    // ==========================================
    private static final String SYSTEM_PROMPT = """
            You are an expert football match analyst. Your job is to analyze upcoming football matches
            and provide data-driven predictions. You will be given team statistics, form, head-to-head
            records, and league context. Provide your analysis as structured JSON only.
            
            Be objective and base predictions on the statistical data provided. Include confidence
            levels (0.0 to 1.0) that honestly reflect the certainty of each prediction.
            Always provide brief but insightful reasoning for each prediction.
            """;

    private static final String RESPONSE_FORMAT_INSTRUCTIONS = """
            Respond ONLY with valid JSON in this exact structure (no markdown, no extra text):
            {
              "match_result": {
                "prediction": "1 or X or 2 — use '1,X' or 'X,2' or '1,2' if genuinely uncertain between two outcomes",
                "confidence": 0.0-1.0,
                "reasoning": "brief explanation"
              },
              "btts": {
                "prediction": true or false,
                "confidence": 0.0-1.0,
                "reasoning": "brief explanation"
              },
              "more_goals_2nd_half": {
                "prediction": true or false,
                "confidence": 0.0-1.0,
                "reasoning": "brief explanation"
              },
              "most_shots_on_goal": {
                "prediction": "HOME or AWAY",
                "reasoning": "brief explanation"
              },
              "most_yellow_cards": {
                "prediction": "HOME or AWAY",
                "reasoning": "brief explanation"
              },
              "most_corners": {
                "prediction": "HOME or AWAY",
                "reasoning": "brief explanation"
              },
              "data_basis": "one or two sentences summarising the key statistics and data used for all predictions (form, H2H, league position, etc.)"
            }
            """;

    private static final String BET_BUILDER_SYSTEM_PROMPT = """
            You are an expert football bet builder analyst. Your task is to analyze a set of upcoming
            football matches and build a 4-match accumulator coupon using bet builder selections.

            For each match provided, you must suggest up to 3 bet builder options. Each option is a
            combination of individual markets (e.g. "Home Win + BTTS + Over 2.5 Goals") that together
            produce an estimated combined odds of approximately 8-12 (target ~10).

            Valid individual selections and their approximate base odds:
            - HOME_WIN (~1.5-3.5), DRAW (~3-4), AWAY_WIN (~1.5-3.5)
            - BTTS_YES (~1.8-2.2), BTTS_NO (~1.6-2.0)
            - OVER_2_5 (~1.8-2.2), UNDER_2_5 (~1.6-2.0)
            - OVER_3_5 (~2.5-3.5), UNDER_3_5 (~1.3-1.6)
            - FIRST_HALF_OVER_1_5 (~2.0-3.0)
            - HOME_TO_SCORE (~1.3-1.6), AWAY_TO_SCORE (~1.4-1.8)
            - WIN_TO_NIL_HOME (~2.5-4.0), WIN_TO_NIL_AWAY (~3.0-5.0)

            After generating options for all matches, SELECT the best 4 matches for the coupon.
            Pick matches where the predictions are most confident and the combined odds across the
            4 recommended options would make an attractive accumulator.

            Base your predictions on the team form, statistics, and head-to-head data provided.
            Be realistic — only suggest options where the statistical data supports the prediction.
            """;

    private static final String BET_BUILDER_RESPONSE_FORMAT = """
            Respond ONLY with valid JSON in this exact structure (no markdown, no extra text):
            {
              "selected_matches": [
                {
                  "match_id": <number>,
                  "bet_options": [
                    {
                      "description": "Home Win + BTTS + Over 2.5",
                      "selections": ["HOME_WIN", "BTTS_YES", "OVER_2_5"],
                      "estimated_odds": 9.5,
                      "confidence": 0.65,
                      "reasoning": "brief statistical reasoning"
                    }
                  ],
                  "recommended_option": 0
                }
              ],
              "overall_reasoning": "brief explanation of why these 4 matches were selected"
            }

            Rules:
            - Include exactly 4 matches in selected_matches (the best 4 from the submitted list).
            - Each match must have 1 to 3 bet_options.
            - estimated_odds for each option should be in the range 7-13, targeting ~10.
            - recommended_option is the 0-based index of the bet_option you recommend most.
            - selections must use only the valid selection identifiers listed above.
            - match_id must exactly match the IDs provided in the input.
            """;

    private static final String STRYKTIPSET_SYSTEM_PROMPT = """
            You are an expert football match analyst specializing in the Swedish Stryktipset betting game.
            You will be given 13 football matches with odds and "Svenska Folket" (public betting distribution).
            For each match, predict the outcome: "1" (home win), "X" (draw), or "2" (away win).

            Use the odds and public distribution as signals. Lower odds indicate the bookmaker's favorite.
            Look for value — matches where the public is heavily biased one way but odds suggest otherwise.
            Consider that draws are often undervalued by the public. Be objective and data-driven.
            Include confidence levels (0.0 to 1.0) and brief reasoning for each prediction.
            """;

    private static final String STRYKTIPSET_RESPONSE_FORMAT = """
            Respond ONLY with valid JSON in this exact structure (no markdown, no extra text):
            {
              "predictions": [
                {
                  "event_number": 1,
                  "prediction": "1 or X or 2",
                  "confidence": 0.0-1.0,
                  "reasoning": "brief explanation"
                },
                {
                  "event_number": 2,
                  "prediction": "1 or X or 2",
                  "confidence": 0.0-1.0,
                  "reasoning": "brief explanation"
                }
              ]
            }
            Include all 13 matches in the predictions array, ordered by event_number (1-13).
            """;
}
