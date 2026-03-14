package com.footballanalyzer.service;

import com.footballanalyzer.model.dto.Dtos.AccuracyOverviewDto;
import com.footballanalyzer.model.entity.*;
import com.footballanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccuracyService {

    private final AnalysisRepository analysisRepository;
    private final MatchResultRepository matchResultRepository;
    private final PredictionAccuracyRepository predictionAccuracyRepository;

    /**
     * Evaluate all analyses that have results but no accuracy record yet.
     */
    @Transactional
    public int evaluateAll() {
        List<Analysis> analyses = analysisRepository.findAll();
        int evaluated = 0;

        for (Analysis analysis : analyses) {
            Match match = analysis.getMatch();

            // Skip if already evaluated
            if (predictionAccuracyRepository.findByMatchId(match.getId()).isPresent()) {
                continue;
            }

            // Skip if no result
            MatchResult result = matchResultRepository.findByMatchId(match.getId()).orElse(null);
            if (result == null) continue;

            PredictionAccuracy accuracy = evaluate(analysis, result);
            predictionAccuracyRepository.save(accuracy);
            evaluated++;
        }

        log.info("Evaluated {} predictions", evaluated);
        return evaluated;
    }

    /**
     * Evaluate a single analysis against its match result.
     */
    public PredictionAccuracy evaluate(Analysis analysis, MatchResult result) {
        PredictionAccuracy accuracy = PredictionAccuracy.builder()
                .analysis(analysis)
                .match(analysis.getMatch())
                .build();

        // Result correct? Handles both single ("1") and dual ("1,X") predictions
        String pred = analysis.getResultPrediction();
        String actual = result.getFullTimeResult();
        accuracy.setResultCorrect(pred.equals(actual) || pred.contains(actual));

        // BTTS correct?
        accuracy.setBttsCorrect(
                analysis.getBttsPrediction().equals(result.getBtts()));

        // More goals 2nd half correct?
        if (result.getMoreGoals2ndHalf() != null) {
            accuracy.setMoreGoals2ndHalfCorrect(
                    analysis.getMoreGoals2ndHalfPrediction().equals(result.getMoreGoals2ndHalf()));
        }

        // Corners: correct team with most corners?
        if (result.getHomeCorners() != null && result.getAwayCorners() != null
                && analysis.getMostCorners() != null) {
            String actualMostCorners = result.getHomeCorners() >= result.getAwayCorners() ? "HOME" : "AWAY";
            accuracy.setCornersCorrect(analysis.getMostCorners().equals(actualMostCorners));
        }

        // Yellow cards: correct team with most yellow cards?
        if (result.getHomeYellowCards() != null && result.getAwayYellowCards() != null
                && analysis.getMostYellowCards() != null) {
            String actualMostYellowCards = result.getHomeYellowCards() >= result.getAwayYellowCards() ? "HOME" : "AWAY";
            accuracy.setYellowCardsCorrect(analysis.getMostYellowCards().equals(actualMostYellowCards));
        }

        // Shots on goal: no data available from free tier — left null

        accuracy.calculateScore();
        return accuracy;
    }

    /**
     * Get accuracy overview, optionally filtered by league.
     */
    public AccuracyOverviewDto getOverview(Long leagueId) {
        Object[] raw = predictionAccuracyRepository.getAccuracyOverview(leagueId);

        if (raw == null || raw.length == 0 || raw[0] == null) {
            return AccuracyOverviewDto.builder()
                    .totalMatches(0)
                    .build();
        }

        // The query returns: resultAvg, bttsAvg, moreGoals2ndHalfAvg, yellowCardsAvg, cornersAvg, count
        Object[] row = (raw[0] instanceof Object[]) ? (Object[]) raw[0] : raw;

        BigDecimal resultAcc = toBigDecimalPercent(row[0]);
        BigDecimal bttsAcc = toBigDecimalPercent(row[1]);
        BigDecimal moreGoals2ndHalfAcc = toBigDecimalPercent(row[2]);
        BigDecimal yellowCardsAcc = toBigDecimalPercent(row[3]);
        BigDecimal cornersAcc = toBigDecimalPercent(row[4]);
        Integer totalMatches = ((Number) row[5]).intValue();

        // Overall average (5 fields — shots excluded as data unavailable)
        BigDecimal overall = resultAcc.add(bttsAcc).add(moreGoals2ndHalfAcc)
                .add(yellowCardsAcc).add(cornersAcc)
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);

        return AccuracyOverviewDto.builder()
                .totalMatches(totalMatches)
                .resultAccuracy(resultAcc)
                .bttsAccuracy(bttsAcc)
                .moreGoals2ndHalfAccuracy(moreGoals2ndHalfAcc)
                .yellowCardsAccuracy(yellowCardsAcc)
                .cornersAccuracy(cornersAcc)
                .overallAccuracy(overall)
                .build();
    }

    private BigDecimal toBigDecimalPercent(Object val) {
        if (val == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(((Number) val).doubleValue() * 100)
                .setScale(1, RoundingMode.HALF_UP);
    }
}
