package com.footballanalyzer.controller;

import com.footballanalyzer.config.FootballDataConfig;
import com.footballanalyzer.integration.footballdata.FootballDataClient;
import com.footballanalyzer.service.AccuracyService;
import com.footballanalyzer.service.AnalysisService;
import com.footballanalyzer.service.FixtureService;
import com.footballanalyzer.service.ResultService;
import com.footballanalyzer.service.StryktipsetService;
import com.footballanalyzer.service.TeamFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FixtureService fixtureService;
    private final ResultService resultService;
    private final AnalysisService analysisService;
    private final AccuracyService accuracyService;
    private final StryktipsetService stryktipsetService;
    private final TeamFormService teamFormService;
    private final FootballDataClient footballDataClient;
    private final FootballDataConfig footballDataConfig;

    /**
     * Manually trigger fixture sync from API-Football.
     */
    @PostMapping("/sync/fixtures")
    public ResponseEntity<Map<String, Object>> syncFixtures() {
        int synced = fixtureService.syncUpcomingFixtures();
        return ResponseEntity.ok(Map.of(
                "message", "Fixture sync completed",
                "fixturesSynced", synced
        ));
    }

    /**
     * Manually trigger result sync for finished matches.
     */
    @PostMapping("/sync/results")
    public ResponseEntity<Map<String, Object>> syncResults() {
        int synced = resultService.syncResults();
        return ResponseEntity.ok(Map.of(
                "message", "Result sync completed",
                "resultsSynced", synced
        ));
    }

    /**
     * Manually trigger league standings sync → populates team_form table.
     */
    @PostMapping("/sync/form")
    public ResponseEntity<Map<String, Object>> syncForm() {
        int synced = teamFormService.syncStandings();
        return ResponseEntity.ok(Map.of(
                "message", "Team form sync completed",
                "teamFormsSynced", synced
        ));
    }

    /**
     * Manually trigger AI analysis for all weekend matches.
     */
    @PostMapping("/sync/analysis")
    public ResponseEntity<Map<String, Object>> syncAnalysis() {
        int analyzed = analysisService.generateWeekendAnalyses();
        return ResponseEntity.ok(Map.of(
                "message", "Analysis generation completed",
                "matchesAnalyzed", analyzed
        ));
    }

    /**
     * Manually trigger accuracy evaluation.
     */
    @PostMapping("/sync/accuracy")
    public ResponseEntity<Map<String, Object>> syncAccuracy() {
        int evaluated = accuracyService.evaluateAll();
        return ResponseEntity.ok(Map.of(
                "message", "Accuracy evaluation completed",
                "matchesEvaluated", evaluated
        ));
    }

    /**
     * Manually trigger Stryktipset coupon sync from Svenska Spel.
     */
    @PostMapping("/sync/stryktipset")
    public ResponseEntity<Map<String, Object>> syncStryktipset() {
        int synced = stryktipsetService.syncCoupon();
        return ResponseEntity.ok(Map.of(
                "message", "Stryktipset sync completed",
                "eventsSynced", synced
        ));
    }

    /**
     * Manually trigger AI predictions for Stryktipset.
     */
    @PostMapping("/sync/stryktipset/predictions")
    public ResponseEntity<Map<String, Object>> syncStryktipsetPredictions() {
        int predicted = stryktipsetService.generatePredictions();
        return ResponseEntity.ok(Map.of(
                "message", "Stryktipset prediction generation completed",
                "eventsPredicted", predicted
        ));
    }

    /**
     * Manually trigger 128kr coupon generation (7 halvgarderingar) for Stryktipset.
     */
    @PostMapping("/sync/stryktipset/coupon")
    public ResponseEntity<Map<String, Object>> syncStryktipsetCoupon() {
        int saved = stryktipsetService.generateCoupon();
        return ResponseEntity.ok(Map.of(
                "message", "Stryktipset coupon generation completed",
                "eventsSaved", saved
        ));
    }

    /**
     * Debug: returns the raw JSON response from football-data.org for a given competition.
     * Call: GET /api/admin/debug/football-data/PL
     */
    @GetMapping(value = "/debug/football-data/{code}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> debugFootballData(@PathVariable String code) {
        String raw = footballDataClient.getMatchesRaw(code.toUpperCase(), footballDataConfig.getCurrentMatchday());
        return ResponseEntity.ok(raw);
    }

    /**
     * Run the full pipeline: fixtures → analysis → results → accuracy.
     */
    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        int fixtures = fixtureService.syncUpcomingFixtures();
        int analyses = analysisService.generateWeekendAnalyses();
        int results = resultService.syncResults();
        int accuracy = accuracyService.evaluateAll();

        return ResponseEntity.ok(Map.of(
                "fixturesSynced", fixtures,
                "matchesAnalyzed", analyses,
                "resultsSynced", results,
                "matchesEvaluated", accuracy
        ));
    }
}
