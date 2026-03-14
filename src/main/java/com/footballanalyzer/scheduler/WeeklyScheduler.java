package com.footballanalyzer.scheduler;

import com.footballanalyzer.service.AccuracyService;
import com.footballanalyzer.service.AnalysisService;
import com.footballanalyzer.service.FixtureService;
import com.footballanalyzer.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyScheduler {

    private final FixtureService fixtureService;
    private final AnalysisService analysisService;
    private final ResultService resultService;
    private final AccuracyService accuracyService;

    /**
     * Friday 08:00 UTC — Fetch upcoming weekend fixtures for all leagues.
     */
    @Scheduled(cron = "0 0 8 * * FRI", zone = "UTC")
    public void fetchFixtures() {
        log.info("=== SCHEDULED: Fetching weekend fixtures ===");
        try {
            int synced = fixtureService.syncUpcomingFixtures();
            log.info("Scheduled fixture sync complete: {} fixtures synced", synced);
        } catch (Exception e) {
            log.error("Scheduled fixture sync failed", e);
        }
    }

    /**
     * Friday 10:00 UTC — Generate AI analysis for all weekend matches.
     */
    @Scheduled(cron = "0 0 10 * * FRI", zone = "UTC")
    public void generateAnalyses() {
        log.info("=== SCHEDULED: Generating weekend analyses ===");
        try {
            int analyzed = analysisService.generateWeekendAnalyses();
            log.info("Scheduled analysis generation complete: {} matches analyzed", analyzed);
        } catch (Exception e) {
            log.error("Scheduled analysis generation failed", e);
        }
    }

    /**
     * Monday 08:00 UTC — Fetch match results and statistics.
     */
    @Scheduled(cron = "0 0 8 * * MON", zone = "UTC")
    public void fetchResults() {
        log.info("=== SCHEDULED: Fetching match results ===");
        try {
            int synced = resultService.syncResults();
            log.info("Scheduled result sync complete: {} results synced", synced);
        } catch (Exception e) {
            log.error("Scheduled result sync failed", e);
        }
    }

    /**
     * Monday 12:00 UTC — Evaluate prediction accuracy.
     */
    @Scheduled(cron = "0 0 12 * * MON", zone = "UTC")
    public void evaluateAccuracy() {
        log.info("=== SCHEDULED: Evaluating prediction accuracy ===");
        try {
            int evaluated = accuracyService.evaluateAll();
            log.info("Scheduled accuracy evaluation complete: {} predictions evaluated", evaluated);
        } catch (Exception e) {
            log.error("Scheduled accuracy evaluation failed", e);
        }
    }
}
