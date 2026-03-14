package com.footballanalyzer.service;

import com.footballanalyzer.integration.claude.AIClient;
import com.footballanalyzer.integration.claude.ClaudeAIClient;
import com.footballanalyzer.integration.svenskaspel.StryktipsetClient;
import com.footballanalyzer.integration.svenskaspel.dto.StryktipsetApiDtos.*;
import com.footballanalyzer.model.entity.StryktipsetDraw;
import com.footballanalyzer.model.entity.StryktipsetEvent;
import com.footballanalyzer.repository.StryktipsetDrawRepository;
import com.footballanalyzer.repository.StryktipsetEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StryktipsetService {

    private final StryktipsetClient stryktipsetClient;
    private final AIClient claudeAIClient;
    private final StryktipsetDrawRepository drawRepository;
    private final StryktipsetEventRepository eventRepository;

    /**
     * Fetch the current Stryktipset coupon from Svenska Spel and store it.
     */
    @Transactional
    public int syncCoupon() {
        DrawsResponse response = stryktipsetClient.fetchDraws();
        if (response == null || response.getDraws() == null || response.getDraws().isEmpty()) {
            log.warn("No Stryktipset draws returned from API");
            return 0;
        }

        int synced = 0;
        for (Draw apiDraw : response.getDraws()) {
            synced += syncDraw(apiDraw);
        }
        return synced;
    }

    private int syncDraw(Draw apiDraw) {
        StryktipsetDraw draw = drawRepository.findByDrawNumber(apiDraw.getDrawNumber())
                .orElseGet(() -> {
                    StryktipsetDraw newDraw = new StryktipsetDraw();
                    newDraw.setDrawNumber(apiDraw.getDrawNumber());
                    return newDraw;
                });

        draw.setDrawState(apiDraw.getDrawState());
        draw.setRegCloseTime(parseDateTime(apiDraw.getRegCloseTime()));
        draw = drawRepository.save(draw);

        if (apiDraw.getDrawEvents() == null) {
            return 0;
        }

        int eventCount = 0;
        for (DrawEvent apiEvent : apiDraw.getDrawEvents()) {
            syncEvent(draw, apiEvent);
            eventCount++;
        }

        log.info("Synced draw #{} with {} events (state={})",
                apiDraw.getDrawNumber(), eventCount, apiDraw.getDrawState());
        return eventCount;
    }

    private void syncEvent(StryktipsetDraw draw, DrawEvent apiEvent) {
        StryktipsetEvent event = eventRepository.findByDrawIdOrderByEventNumberAsc(draw.getId())
                .stream()
                .filter(e -> e.getEventNumber().equals(apiEvent.getEventNumber()))
                .findFirst()
                .orElseGet(() -> {
                    StryktipsetEvent newEvent = new StryktipsetEvent();
                    newEvent.setDraw(draw);
                    newEvent.setEventNumber(apiEvent.getEventNumber());
                    return newEvent;
                });

        MatchInfo match = apiEvent.getMatch();
        if (match != null) {
            event.setHomeTeam(match.getHomeTeamName());
            event.setAwayTeam(match.getAwayTeamName());
            event.setLeagueName(match.getLeagueName());
            event.setKickoffTime(parseDateTime(match.getMatchStart()));

            // Update results if match is finished
            if (match.getHomeTeamResult() != null && match.getAwayTeamResult() != null) {
                event.setHomeGoals(match.getHomeTeamResult());
                event.setAwayGoals(match.getAwayTeamResult());
                if (match.getHomeTeamResult() > match.getAwayTeamResult()) {
                    event.setActualResult("1");
                } else if (match.getHomeTeamResult() < match.getAwayTeamResult()) {
                    event.setActualResult("2");
                } else {
                    event.setActualResult("X");
                }
            }
        }

        // Parse odds (Swedish decimal format: "5,60" → 5.60)
        if (apiEvent.getOdds() != null) {
            event.setOdds1(parseSwedishDecimal(apiEvent.getOdds().getOne()));
            event.setOddsX(parseSwedishDecimal(apiEvent.getOdds().getX()));
            event.setOdds2(parseSwedishDecimal(apiEvent.getOdds().getTwo()));
        }

        // Parse svenska folket percentages
        if (apiEvent.getSvenskaFolket() != null) {
            event.setSf1(parseSwedishDecimal(apiEvent.getSvenskaFolket().getOne()));
            event.setSfX(parseSwedishDecimal(apiEvent.getSvenskaFolket().getX()));
            event.setSf2(parseSwedishDecimal(apiEvent.getSvenskaFolket().getTwo()));
        }

        eventRepository.save(event);
    }

    /**
     * Generate AI predictions for all 13 matches in the latest open draw.
     */
    @Transactional
    public int generatePredictions() {
        StryktipsetDraw draw = drawRepository.findLatestOpen().orElse(null);
        if (draw == null) {
            // Fall back to latest draw of any state
            List<StryktipsetDraw> all = drawRepository.findAllByOrderByDrawNumberDesc();
            if (all.isEmpty()) {
                log.warn("No Stryktipset draws found for prediction");
                return 0;
            }
            draw = all.get(0);
        }

        // Always use all events — regenerates predictions even if they already exist
        List<StryktipsetEvent> events = eventRepository.findByDrawIdOrderByEventNumberAsc(draw.getId());
        if (events.isEmpty()) {
            log.warn("No events found for draw #{}", draw.getDrawNumber());
            return 0;
        }

        log.info("Generating AI predictions for {} events in draw #{}", events.size(), draw.getDrawNumber());

        String matchContext = buildMatchContext(draw, events);

        ClaudeAIClient.StryktipsetPredictionsResponse response = claudeAIClient.analyzeStryktipset(matchContext);
        if (response == null || response.getPredictions() == null) {
            log.error("Failed to get Stryktipset predictions from Gemini");
            return 0;
        }

        int predicted = 0;
        for (ClaudeAIClient.StryktipsetPrediction prediction : response.getPredictions()) {
            StryktipsetEvent event = events.stream()
                    .filter(e -> e.getEventNumber().equals(prediction.getEventNumber()))
                    .findFirst()
                    .orElse(null);

            if (event != null) {
                event.setAiPrediction(prediction.getPrediction());
                event.setAiConfidence(prediction.getConfidence() != null
                        ? BigDecimal.valueOf(prediction.getConfidence()) : null);
                event.setAiReasoning(prediction.getReasoning());
                eventRepository.save(event);
                predicted++;
            }
        }

        log.info("Generated {} predictions for draw #{}", predicted, draw.getDrawNumber());
        return predicted;
    }

    /**
     * Generate a 128kr coupon (7 halvgarderingar) for the latest open draw.
     */
    @Transactional
    public int generateCoupon() {
        StryktipsetDraw draw = drawRepository.findLatestOpen().orElse(null);
        if (draw == null) {
            List<StryktipsetDraw> all = drawRepository.findAllByOrderByDrawNumberDesc();
            if (all.isEmpty()) {
                log.warn("No Stryktipset draws found for coupon generation");
                return 0;
            }
            draw = all.get(0);
        }

        // Always use all events — regenerates the coupon even if one already exists
        List<StryktipsetEvent> allEvents = eventRepository.findByDrawIdOrderByEventNumberAsc(draw.getId());
        if (allEvents.isEmpty()) {
            log.warn("No events found for draw #{}", draw.getDrawNumber());
            return 0;
        }
        String matchContext = buildCouponContext(draw, allEvents);

        ClaudeAIClient.StryktipsetCouponResponse response = claudeAIClient.generateStryktipsetCoupon(matchContext);
        if (response == null || response.getPicks() == null) {
            log.error("Failed to get coupon from AI for draw #{}", draw.getDrawNumber());
            return 0;
        }

        int saved = 0;
        for (ClaudeAIClient.StryktipsetCouponPick pick : response.getPicks()) {
            StryktipsetEvent event = allEvents.stream()
                    .filter(e -> e.getEventNumber().equals(pick.getEventNumber()))
                    .findFirst()
                    .orElse(null);

            if (event != null && pick.getSigns() != null && !pick.getSigns().isEmpty()) {
                event.setCouponSigns(String.join(",", pick.getSigns()));
                event.setCouponReasoning(pick.getReasoning());
                eventRepository.save(event);
                saved++;
            }
        }

        log.info("Generated 128kr coupon with {} signs for draw #{} (strategy: {})",
                saved, draw.getDrawNumber(), response.getStrategy());
        return saved;
    }

    private String buildCouponContext(StryktipsetDraw draw, List<StryktipsetEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Stryktipset Draw #%d — build a 128kr coupon (7 halvgarderingar)\n\n",
                draw.getDrawNumber()));

        for (StryktipsetEvent event : events) {
            sb.append(String.format("Match %d: %s vs %s\n", event.getEventNumber(), event.getHomeTeam(), event.getAwayTeam()));
            sb.append(String.format("  League: %s\n", event.getLeagueName() != null ? event.getLeagueName() : "Unknown"));
            if (event.getKickoffTime() != null) {
                sb.append(String.format("  Kickoff: %s\n", event.getKickoffTime()));
            }

            if (event.getOdds1() != null) {
                double o1 = event.getOdds1().doubleValue();
                double oX = event.getOddsX().doubleValue();
                double o2 = event.getOdds2().doubleValue();

                // Raw implied probabilities (bookmaker's true probability estimate)
                double imp1 = (1.0 / o1) * 100;
                double impX = (1.0 / oX) * 100;
                double imp2 = (1.0 / o2) * 100;

                sb.append(String.format("  Odds:             1=%.2f   X=%.2f   2=%.2f\n", o1, oX, o2));
                sb.append(String.format("  Implied prob:     1=%.1f%%  X=%.1f%%  2=%.1f%%\n", imp1, impX, imp2));

                if (event.getSf1() != null) {
                    double sf1 = event.getSf1().doubleValue();
                    double sfX = event.getSfX().doubleValue();
                    double sf2 = event.getSf2().doubleValue();

                    // Discrepancy: positive = public over-backs this outcome vs what odds imply
                    double diff1 = sf1 - imp1;
                    double diffX = sfX - impX;
                    double diff2 = sf2 - imp2;

                    sb.append(String.format("  Svenska Folket:   1=%.0f%%   X=%.0f%%   2=%.0f%%\n", sf1, sfX, sf2));
                    sb.append(String.format("  Public vs odds:   1=%+.1f%%  X=%+.1f%%  2=%+.1f%%\n", diff1, diffX, diff2));

                    // Flag where underdog has value (public hugely overbetting favourite)
                    double maxDiff = Math.max(Math.abs(diff1), Math.max(Math.abs(diffX), Math.abs(diff2)));
                    if (maxDiff >= 10) {
                        String overbet = diff1 >= maxDiff ? "1" : (diff2 >= maxDiff ? "2" : "X");
                        sb.append(String.format("  *** Public is heavily over-backing '%s' (+%.1f%%) — potential value on the other outcomes ***\n", overbet, maxDiff));
                    }
                }
            }

            if (event.getAiPrediction() != null) {
                sb.append(String.format("  AI prediction: %s (confidence: %.0f%%)\n",
                        event.getAiPrediction(),
                        event.getAiConfidence() != null ? event.getAiConfidence().doubleValue() * 100 : 0));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildMatchContext(StryktipsetDraw draw, List<StryktipsetEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Stryktipset Draw #%d (Registration closes: %s)\n\n",
                draw.getDrawNumber(), draw.getRegCloseTime()));

        for (StryktipsetEvent event : events) {
            sb.append(String.format("Match %d: %s vs %s\n", event.getEventNumber(), event.getHomeTeam(), event.getAwayTeam()));
            sb.append(String.format("  League: %s\n", event.getLeagueName() != null ? event.getLeagueName() : "Unknown"));
            if (event.getKickoffTime() != null) {
                sb.append(String.format("  Kickoff: %s\n", event.getKickoffTime()));
            }
            if (event.getOdds1() != null) {
                sb.append(String.format("  Odds: 1=%.2f  X=%.2f  2=%.2f\n",
                        event.getOdds1(), event.getOddsX(), event.getOdds2()));
            }
            if (event.getSf1() != null) {
                sb.append(String.format("  Svenska Folket: 1=%.0f%%  X=%.0f%%  2=%.0f%%\n",
                        event.getSf1(), event.getSfX(), event.getSf2()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private BigDecimal parseSwedishDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.replace(",", ".").trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Swedish decimal: '{}'", value);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: '{}'", dateTimeStr);
            return null;
        }
    }
}
