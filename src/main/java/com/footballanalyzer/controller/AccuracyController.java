package com.footballanalyzer.controller;

import com.footballanalyzer.model.dto.Dtos.*;
import com.footballanalyzer.repository.PredictionAccuracyRepository;
import com.footballanalyzer.service.AccuracyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accuracy")
@RequiredArgsConstructor
public class AccuracyController {

    private final AccuracyService accuracyService;
    private final PredictionAccuracyRepository predictionAccuracyRepository;

    /**
     * Get overall accuracy stats, optionally filtered by league.
     */
    @GetMapping("/overview")
    public ResponseEntity<AccuracyOverviewDto> getOverview(
            @RequestParam(required = false) Long leagueId) {
        return ResponseEntity.ok(accuracyService.getOverview(leagueId));
    }

    /**
     * Get accuracy details for a specific gameweek.
     */
    @GetMapping("/gameweek/{gameweekId}")
    public ResponseEntity<List<AccuracyDto>> getGameweekAccuracy(@PathVariable Long gameweekId) {
        List<AccuracyDto> accuracies = predictionAccuracyRepository.findByGameweekId(gameweekId).stream()
                .map(pa -> AccuracyDto.builder()
                        .matchId(pa.getMatch().getId())
                        .homeTeam(pa.getMatch().getHomeTeam().getName())
                        .awayTeam(pa.getMatch().getAwayTeam().getName())
                        .resultCorrect(pa.getResultCorrect())
                        .bttsCorrect(pa.getBttsCorrect())
                        .moreGoals2ndHalfCorrect(pa.getMoreGoals2ndHalfCorrect())
                        .shotsCorrect(pa.getShotsCorrect())
                        .yellowCardsCorrect(pa.getYellowCardsCorrect())
                        .cornersCorrect(pa.getCornersCorrect())
                        .score(pa.getScore())
                        .build())
                .toList();
        return ResponseEntity.ok(accuracies);
    }
}
