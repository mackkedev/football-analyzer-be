package com.footballanalyzer.service;

import com.footballanalyzer.config.FootballDataConfig;
import com.footballanalyzer.integration.footballdata.FootballDataClient;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.Head2Head;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.SingleMatchResponse;
import com.footballanalyzer.integration.claude.AIClient;
import com.footballanalyzer.integration.claude.ClaudeAIClient.AnalysisResponse;
import com.footballanalyzer.integration.claude.PromptBuilder;
import com.footballanalyzer.model.entity.*;
import com.footballanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AIClient aiClient;
    private final FootballDataClient footballDataClient;
    private final PromptBuilder promptBuilder;
    private final MatchRepository matchRepository;
    private final AnalysisRepository analysisRepository;
    private final TeamFormRepository teamFormRepository;
    private final FootballDataConfig footballDataConfig;

    /**
     * Generate (or regenerate) AI analysis for all upcoming weekend matches.
     * Deletes any existing analyses in the window first so re-running always produces fresh results.
     */
    @Transactional
    public int generateWeekendAnalyses() {
        LocalDateTime weekendStart = getNextWeekendStart();
        LocalDateTime weekendEnd = getNextWeekendEnd();

        // Delete existing analyses in the window so we can regenerate with latest form data
        List<Analysis> existing = analysisRepository.findWeekendAnalyses(weekendStart, weekendEnd);
        if (!existing.isEmpty()) {
            analysisRepository.deleteAll(existing);
            log.info("Deleted {} existing analyses to regenerate", existing.size());
        }

        List<Match> matches = matchRepository.findScheduledWithoutAnalysis(weekendStart, weekendEnd);
        log.info("Found {} matches to analyze for weekend {} - {}", matches.size(), weekendStart, weekendEnd);

        int analyzed = 0;
        for (Match match : matches) {
            try {
                generateAnalysis(match);
                analyzed++;
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Failed to analyze match {} vs {}: {}",
                        match.getHomeTeam().getName(), match.getAwayTeam().getName(), e.getMessage());
            }
        }

        log.info("Generated {} analyses", analyzed);
        return analyzed;
    }

    /**
     * Generate AI analysis for a single match.
     */
    @Transactional
    public Analysis generateAnalysis(Match match) {
        analysisRepository.findByMatchId(match.getId()).ifPresent(existing -> {
            log.info("Deleting existing analysis for match {} to regenerate", match.getId());
            analysisRepository.delete(existing);
        });

        int season = footballDataConfig.getCurrentSeason();
        TeamForm homeForm = teamFormRepository
                .findByTeamIdAndLeagueIdAndSeason(match.getHomeTeam().getId(), match.getLeague().getId(), season)
                .orElse(null);
        TeamForm awayForm = teamFormRepository
                .findByTeamIdAndLeagueIdAndSeason(match.getAwayTeam().getId(), match.getLeague().getId(), season)
                .orElse(null);

        // Get H2H aggregate from football-data.org
        Head2Head h2h = null;
        try {
            SingleMatchResponse matchResponse = footballDataClient.getMatch(match.getApiFixtureId());
            if (matchResponse != null) {
                h2h = matchResponse.getHead2head();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch H2H data: {}", e.getMessage());
        }

        String context = promptBuilder.buildMatchContext(match, homeForm, awayForm, h2h);
        AnalysisResponse response = aiClient.analyzeMatch(context);

        if (response == null) {
            log.error("No response from AI for match {}", match.getId());
            return null;
        }

        Analysis analysis = mapToAnalysis(match, response);
        analysisRepository.save(analysis);

        log.info("Analysis generated for {} vs {}: result={}, btts={}, o/u={}",
                match.getHomeTeam().getName(), match.getAwayTeam().getName(),
                analysis.getResultPrediction(), analysis.getBttsPrediction(),
                analysis.getGoalsOverUnderPrediction());

        return analysis;
    }

    private Analysis mapToAnalysis(Match match, AnalysisResponse response) {
        Analysis.AnalysisBuilder builder = Analysis.builder()
                .match(match)
                .modelUsed(response.getModel())
                .rawAiResponse(response.getRawResponse());

        if (response.getMatchResult() != null) {
            builder.resultPrediction(response.getMatchResult().getPrediction())
                    .resultConfidence(BigDecimal.valueOf(response.getMatchResult().getConfidence()));
        }

        if (response.getBtts() != null) {
            builder.bttsPrediction(response.getBtts().getPrediction())
                    .bttsConfidence(BigDecimal.valueOf(response.getBtts().getConfidence()));
        }

        if (response.getMoreGoals2ndHalf() != null) {
            builder.moreGoals2ndHalfPrediction(response.getMoreGoals2ndHalf().getPrediction())
                    .moreGoals2ndHalfConfidence(BigDecimal.valueOf(response.getMoreGoals2ndHalf().getConfidence()));
        }

        if (response.getMostShotsOnGoal() != null) {
            builder.mostShotsOnGoal(response.getMostShotsOnGoal().getPrediction());
        }

        if (response.getMostYellowCards() != null) {
            builder.mostYellowCards(response.getMostYellowCards().getPrediction());
        }

        if (response.getMostCorners() != null) {
            builder.mostCorners(response.getMostCorners().getPrediction());
        }

        StringBuilder reasoning = new StringBuilder();
        if (response.getDataBasis() != null) reasoning.append("DATA BASIS: ").append(response.getDataBasis()).append("\n\n");
        if (response.getMatchResult() != null) reasoning.append("RESULT: ").append(response.getMatchResult().getReasoning()).append("\n\n");
        if (response.getBtts() != null) reasoning.append("BTTS: ").append(response.getBtts().getReasoning()).append("\n\n");
        if (response.getMoreGoals2ndHalf() != null) reasoning.append("2ND HALF GOALS: ").append(response.getMoreGoals2ndHalf().getReasoning()).append("\n\n");
        if (response.getMostShotsOnGoal() != null) reasoning.append("SHOTS ON GOAL: ").append(response.getMostShotsOnGoal().getReasoning()).append("\n\n");
        if (response.getMostYellowCards() != null) reasoning.append("YELLOW CARDS: ").append(response.getMostYellowCards().getReasoning()).append("\n\n");
        if (response.getMostCorners() != null) reasoning.append("CORNERS: ").append(response.getMostCorners().getReasoning());
        builder.reasoning(reasoning.toString());

        return builder.build();
    }

    private LocalDateTime getNextWeekendStart() {
        LocalDate today = LocalDate.now();
        LocalDate friday = today.with(DayOfWeek.FRIDAY);
        if (today.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
            friday = today;
        }
        return friday.atTime(LocalTime.MIDNIGHT);
    }

    private LocalDateTime getNextWeekendEnd() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY).plusWeeks(
                today.getDayOfWeek() == DayOfWeek.MONDAY ? 1 : 0);
        if (today.getDayOfWeek().getValue() >= DayOfWeek.FRIDAY.getValue()) {
            monday = today.plusDays(8 - today.getDayOfWeek().getValue());
        }
        return monday.atTime(LocalTime.of(23, 59));
    }
}
