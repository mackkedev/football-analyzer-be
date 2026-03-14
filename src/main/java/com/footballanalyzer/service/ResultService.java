package com.footballanalyzer.service;

import com.footballanalyzer.integration.footballdata.FootballDataClient;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.*;
import com.footballanalyzer.model.entity.Match;
import com.footballanalyzer.model.entity.MatchResult;
import com.footballanalyzer.repository.MatchRepository;
import com.footballanalyzer.repository.MatchResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final FootballDataClient footballDataClient;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    /**
     * Fetch results for all finished matches that don't have results yet.
     */
    @Transactional
    public int syncResults() {
        List<Match> matches = matchRepository.findFinishedWithoutResult();
        int synced = 0;

        for (Match match : matches) {
            try {
                syncMatchResult(match);
                synced++;
            } catch (Exception e) {
                log.error("Failed to sync result for match {}: {}", match.getId(), e.getMessage());
            }
        }

        log.info("Synced {} match results", synced);
        return synced;
    }

    /**
     * Fetch and store result for a single match using football-data.org.
     * Note: corners and cards are not available on the free tier.
     */
    @Transactional
    public void syncMatchResult(Match match) {
        SingleMatchResponse response = footballDataClient.getMatch(match.getApiFixtureId());
        if (response == null || response.getMatch() == null) {
            log.warn("No data returned for match {}", match.getApiFixtureId());
            return;
        }

        Score score = response.getMatch().getScore();
        if (score == null || score.getFullTime() == null) {
            log.warn("No score data for match {}", match.getApiFixtureId());
            return;
        }

        int homeGoalsTotal = score.getFullTime().getHome() != null ? score.getFullTime().getHome() : 0;
        int awayGoalsTotal = score.getFullTime().getAway() != null ? score.getFullTime().getAway() : 0;

        Integer homeGoals1st = null;
        Integer awayGoals1st = null;
        Integer homeGoals2nd = null;
        Integer awayGoals2nd = null;

        if (score.getHalfTime() != null) {
            homeGoals1st = score.getHalfTime().getHome();
            awayGoals1st = score.getHalfTime().getAway();
            if (homeGoals1st != null && awayGoals1st != null) {
                homeGoals2nd = homeGoalsTotal - homeGoals1st;
                awayGoals2nd = awayGoalsTotal - awayGoals1st;
            }
        }

        MatchResult result = MatchResult.builder()
                .match(match)
                .homeGoalsTotal(homeGoalsTotal)
                .awayGoalsTotal(awayGoalsTotal)
                .homeGoals1stHalf(homeGoals1st)
                .awayGoals1stHalf(awayGoals1st)
                .homeGoals2ndHalf(homeGoals2nd)
                .awayGoals2ndHalf(awayGoals2nd)
                .goalsTotal(BigDecimal.valueOf(homeGoalsTotal + awayGoalsTotal))
                .build();

        result.calculateDerivedFields();
        matchResultRepository.save(result);

        log.info("Saved result for {} {} - {} {}",
                match.getHomeTeam().getName(), homeGoalsTotal,
                awayGoalsTotal, match.getAwayTeam().getName());
    }
}
