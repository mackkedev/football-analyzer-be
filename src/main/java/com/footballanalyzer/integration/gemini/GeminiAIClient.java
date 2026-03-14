package com.footballanalyzer.integration.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballanalyzer.config.GeminiAIConfig;
import com.footballanalyzer.integration.claude.AIClient;
import com.footballanalyzer.integration.claude.ClaudeAIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Primary
@Component
public class GeminiAIClient implements AIClient {

    private final WebClient webClient;
    private final GeminiAIConfig config;
    private final ObjectMapper objectMapper;

    public GeminiAIClient(@Qualifier("geminiWebClient") WebClient webClient,
                          GeminiAIConfig config,
                          ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClaudeAIClient.AnalysisResponse analyzeMatch(String matchContext) {
        log.info("Sending match analysis request to Gemini (model={})", config.getModel());

        Map<String, Object> requestBody = buildRequest(MATCH_SYSTEM_PROMPT,
                matchContext + "\n\n" + MATCH_RESPONSE_FORMAT);

        try {
            String rawText = callGemini(requestBody);
            if (rawText == null) return null;

            String jsonStr = extractJson(rawText);
            ClaudeAIClient.AnalysisResponse response = objectMapper.readValue(jsonStr, ClaudeAIClient.AnalysisResponse.class);
            response.setRawResponse(rawText);
            response.setModel(config.getModel());
            return response;
        } catch (Exception e) {
            log.error("Failed to get match analysis from Gemini", e);
            return null;
        }
    }

    @Override
    public ClaudeAIClient.StryktipsetPredictionsResponse analyzeStryktipset(String matchContext) {
        log.info("Sending Stryktipset analysis request to Gemini (model={})", config.getModel());

        Map<String, Object> requestBody = buildRequest(STRYKTIPSET_SYSTEM_PROMPT,
                matchContext + "\n\n" + STRYKTIPSET_RESPONSE_FORMAT);

        try {
            String rawText = callGemini(requestBody);
            if (rawText == null) return null;

            String jsonStr = extractJson(rawText);
            return objectMapper.readValue(jsonStr, ClaudeAIClient.StryktipsetPredictionsResponse.class);
        } catch (Exception e) {
            log.error("Failed to get Stryktipset analysis from Gemini", e);
            return null;
        }
    }

    @Override
    public ClaudeAIClient.BetBuilderResponse analyzeBetBuilder(String matchContext) {
        log.info("Sending bet builder analysis request to Gemini (model={})", config.getModel());

        Map<String, Object> requestBody = buildRequest(BET_BUILDER_SYSTEM_PROMPT,
                matchContext + "\n\n" + BET_BUILDER_RESPONSE_FORMAT, 6000);

        String rawText = null;
        try {
            rawText = callGemini(requestBody);
            if (rawText == null) return null;

            log.debug("Gemini bet builder raw response: {}", rawText);
            String jsonStr = extractJson(rawText);
            ClaudeAIClient.BetBuilderResponse response = objectMapper.readValue(jsonStr, ClaudeAIClient.BetBuilderResponse.class);
            response.setModel(config.getModel());
            return response;
        } catch (Exception e) {
            log.error("Failed to parse bet builder response from Gemini. Raw: {}", rawText, e);
            return null;
        }
    }

    @Override
    public ClaudeAIClient.StryktipsetCouponResponse generateStryktipsetCoupon(String matchContext) {
        log.info("Sending Stryktipset coupon request to Gemini (model={})", config.getModel());

        Map<String, Object> requestBody = buildRequest(COUPON_SYSTEM_PROMPT,
                matchContext + "\n\n" + COUPON_RESPONSE_FORMAT, 8192);

        String rawText = null;
        try {
            rawText = callGemini(requestBody);
            if (rawText == null) return null;

            log.info("Gemini coupon raw response: {}", rawText);
            String jsonStr = extractJson(rawText);
            return objectMapper.readValue(jsonStr, ClaudeAIClient.StryktipsetCouponResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Stryktipset coupon from Gemini. Raw response was: {}", rawText, e);
            return null;
        }
    }

    private String callGemini(Map<String, Object> requestBody) {
        String uri = "/v1beta/models/" + config.getModel() + ":generateContent?key=" + config.getApiKey();

        GeminiResponse response = webClient.post()
                .uri(uri)
                .bodyValue(requestBody)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().isError()) {
                        return clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Gemini API error ({}): {}", clientResponse.statusCode(), body))
                                .then(reactor.core.publisher.Mono.empty());
                    }
                    return clientResponse.bodyToMono(GeminiResponse.class);
                })
                .block();

        if (response != null
                && response.getCandidates() != null
                && !response.getCandidates().isEmpty()) {
            GeminiResponse.Candidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() != null
                    && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {
                String text = candidate.getContent().getParts().get(0).getText();
                log.debug("Gemini raw response: {}", text);
                return text;
            }
        }
        log.error("Empty or unexpected response from Gemini");
        return null;
    }

    private Map<String, Object> buildRequest(String systemPrompt, String userMessage) {
        return buildRequest(systemPrompt, userMessage, config.getMaxTokens());
    }

    private Map<String, Object> buildRequest(String systemPrompt, String userMessage, int maxTokens) {
        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", maxTokens
                )
        );
    }

    private String extractJson(String text) {
        String cleaned = text.trim();
        // Strip markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();
        // Find outermost JSON object — handles any stray text before or after the JSON
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    // ==========================================
    // Gemini API Response DTOs
    // ==========================================
    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;

        @lombok.Data
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Candidate {
            private Content content;
            private String finishReason;
        }

        @lombok.Data
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Content {
            private List<Part> parts;
        }

        @lombok.Data
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Part {
            private String text;
        }
    }

    // ==========================================
    // Prompts
    // ==========================================
    private static final String MATCH_SYSTEM_PROMPT = """
            You are an expert football match analyst. Your job is to analyze upcoming football matches
            and provide data-driven predictions. You will be given team statistics, form, head-to-head
            records, and league context. Provide your analysis as structured JSON only.

            Be objective and base predictions on the statistical data provided. Include confidence
            levels (0.0 to 1.0) that honestly reflect the certainty of each prediction.
            Always provide brief but insightful reasoning for each prediction.
            """;

    private static final String MATCH_RESPONSE_FORMAT = """
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

    private static final String COUPON_SYSTEM_PROMPT = """
            You are an expert Swedish Stryktipset coupon strategist. Your mission is to build a
            512 kronor system coupon (9 halvgarderingar + 4 singles) with the goal of winning the
            Stryktipset jackpot of 10,000,000 SEK.
                        
            === COUPON RULES ===
            - Each row costs 1 kr and predicts all 13 matches as 1 (home win), X (draw), or 2 (away win).
            - A "halvgardering" covers 2 outcomes for one match. Exactly 7 halvgarderingar = 2^7 = 128 rows = 128 kr.
            - The remaining 6 matches must have exactly 1 sign (single).
            - Halvgardering combinations: "1,X", "X,2", or "1,2".
                        
            === JACKPOT STRATEGY — THIS IS CRITICAL ===
            The jackpot (10,000,000 SEK) is shared among ALL coupons with 13 correct results.
            If you are the ONLY person with 13 correct results, you win everything.
                        
            This means the goal is NOT just to predict likely outcomes — the goal is to find combinations
            that most bettors will NOT have on their coupon.
                        
            The "Svenska Folket" distribution shows what percentage of all bettors are picking each outcome.
            The "Implied prob" shows what the bookmaker's odds actually imply as the true probability.
                        
            KEY INSIGHT — VALUE AND UNIQUENESS:
            - If an outcome has only 15% Svenska Folket backing and it happens, only ~15% of all
              13-correct coupons will have picked it. This dramatically reduces competition for the jackpot.
            - A match where the favourite has 70% public backing is dangerous: if you pick the favourite
              and they win, 70% of all correct coupons also got it right — you share the jackpot with many.
            - An upset result backed by only 15-20% of the public means you are nearly ALONE if it lands.
                        
            === HOME/AWAY PSYCHOLOGY — THE KEY EDGE ===
            This is where most bettors and even odds models undervalue real-world football:
                        
            **FAVOURITES PLAYING AWAY — THE #1 UPSET SOURCE:**
            - When a top team plays away at a mid-table or lower team, the public massively over-backs
              the favourite because of name recognition and league position.
            - But away games are fundamentally different: no home crowd support, travel fatigue,
              unfamiliar pitch, hostile atmosphere. This is a proven psychological factor.
            - Historically, favourites away win LESS often than their odds imply. The draw is the
              most undervalued outcome when a favourite plays away.
            - RULE: If a clear favourite (top-6 team) is playing AWAY, this is a prime halvgardering
              candidate. Cover the draw (X) alongside the favourite's win — use "X,2" if the away team
              is favourite, or "1,X" to cover the home upset.
            - The bigger the "name gap" between favourite and underdog, the more the public over-backs
              the favourite away — and the more value the draw/upset carries.
                        
            **HOME UNDERDOGS — STRONGER THAN ODDS SAY:**
            - Teams playing at home, even if they are lower in the table, get a psychological and
              tactical boost: home crowd, familiar pitch, defensive setup against stronger opponents.
            - A mid-table home team vs a top team is NOT the pushover the odds suggest.
            - Home teams fighting relegation are especially dangerous — desperation + home crowd = upsets.
            - RULE: Look for home underdogs where the implied home win probability is 20-35% but
              Svenska Folket is only backing them at 10-20%. This is a klassisk "skrällmatch".
                        
            **DERBIES AND RIVALRIES:**
            - In derby matches or local rivalries, form and table position matter less.
              These are chaotic, emotional games where anything can happen.
            - RULE: Always consider a halvgardering on derby/rivalry matches regardless of odds.
                        
            === MATCH CATEGORISATION — DO THIS FIRST ===
            Before selecting halvgarderingar, categorise every match into one of these buckets:
                        
            1. **SKRÄLL-MATCH (Upset candidate)** — Favourite playing AWAY against decent home team,
               OR big public over-backing (60%+ on one outcome when implied prob is only 40-50%).
               → Prime halvgardering target. Cover the upset outcome.
                        
            2. **OSÄKER MATCH (Genuinely uncertain)** — Evenly matched teams, no clear favourite,
               odds close to each other (all outcomes between 25-40% implied prob).
               → Good halvgardering target. Cover the two most likely outcomes.
                        
            3. **KLAR MATCH (Clear favourite)** — One outcome has 55%+ implied probability AND the
               favourite is playing at HOME. Public and odds agree.
               → Use as a single. Pick the clear favourite.
                        
            4. **FÄLLA (Trap match)** — Looks like a "klar match" but has hidden upset factors:
               favourite away, key players missing, congested schedule, nothing to play for.
               → Treat as SKRÄLL-MATCH. Prime halvgardering.
                        
            You need exactly 9 matches in category 1+2+4 (halvgardering) and 6 in category 3 (singles).
            If you have more than 9 upset candidates, prioritise the ones with the biggest
            Svenska Folket vs implied probability gap — those give the most jackpot value.
                        
            === HALVGARDERING PLACEMENT RULES ===
            Priority order for selecting your 9 halvgarderingar:
                        
            1. **Favourites playing away** with 60%+ public backing — ALWAYS halvgardera these.
               Cover favourite win + draw (most common upset pattern for away favourites is a draw).
            2. **Biggest "folket vs odds" discrepancy** — Where public is over-backing an outcome
               by 15+ percentage points versus implied probability.
            3. **Home underdogs in relegation battles** — Desperation + home crowd = upset fuel.
            4. **Derby/rivalry matches** — Chaos factor, ignore the odds.
            5. **Genuinely uncertain matches** — Fill remaining halvgarderingar with the tightest
               odds matches.
                        
            HALVGARDERING SIGN SELECTION:
            - If the favourite is away: prefer "1,X" (home upset or draw) or "X,2" (draw or away win).
              The DRAW is the key sign to include — away favourites draw more than expected.
            - If it's genuinely uncertain: cover the two outcomes with highest implied probability.
            - NEVER exclude the draw on away-favourite matches. The X is where the money is.
                        
            === SINGLES — THE ANCHORS ===
            - Use singles on the 6 clearest matches where:
              a) One outcome has 55%+ implied probability
              b) The favourite is playing at HOME (home advantage reinforces the odds)
              c) No major upset signals (no key injuries, no derby chaos, no end-of-season dead rubber)
            - These are your "safe" legs. They need to land for the coupon to work.
            - Double-check: if a "clear" match has the favourite AWAY, reconsider — it might be a fälla.
                        
            === OUTPUT ===
            Do all your reasoning internally. Output ONLY valid JSON as specified below.
            Put your per-match analysis (category, home/away logic, value reasoning) inside
            each pick's "reasoning" field — not as free text outside the JSON.

            === REMEMBER ===
            - Du spelar INTE för att få 8 eller 10 rätt. Du spelar för 13 rätt och ensam utdelning.
            - Alla har favoriterna. Pengarna ligger i att ha med skrällarna som ingen annan har.
            - Favoriter borta är den största källan till skrällar i europeisk fotboll.
            - X (oavgjort) är det mest undervärderade tecknet på Stryktipset — särskilt för
              bortafavoriter. Folket hatar att spela X, men X är din bästa vän för jackpotten.
            """;

    private static final String COUPON_RESPONSE_FORMAT = """
            Respond ONLY with valid JSON in this exact structure (no markdown, no extra text):
            {
              "picks": [
                {
                  "event_number": 1,
                  "signs": ["2"],
                  "value_rating": "LOW",
                  "reasoning": "Man City heavy favourite, implied prob 62%, public aligned at 48%, no upset signal. Single on 2."
                },
                {
                  "event_number": 5,
                  "signs": ["1", "X"],
                  "value_rating": "HIGH",
                  "reasoning": "Public backing away team at 60% but implied prob only 45%. Home team undervalued at 1.8 odds. Upset potential — halvgardering covers home win or draw."
                }
              ],
              "total_rows": 128,
              "strategy": "one sentence summary of the overall value strategy"
            }

            Rules:
            - Include all 13 matches ordered by event_number (1-13).
            - Exactly 7 matches must have 2 signs (halvgardering): ["1","X"], ["X","2"], or ["1","2"].
            - Exactly 6 matches must have 1 sign (single): ["1"], ["X"], or ["2"].
            - total_rows must always be 128.
            - value_rating must be "HIGH" (clear upset value), "MEDIUM" (some uncertainty), or "LOW" (confident single).
            - reasoning must explain the value logic, not just state the outcome.
            """;
}
