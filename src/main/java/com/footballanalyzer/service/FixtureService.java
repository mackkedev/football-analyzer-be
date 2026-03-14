package com.footballanalyzer.service;

import com.footballanalyzer.config.FootballDataConfig;
import com.footballanalyzer.integration.footballdata.FootballDataClient;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.MatchesResponse;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.MatchTeam;
import com.footballanalyzer.model.entity.*;
import com.footballanalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixtureService {

    private final FootballDataClient footballDataClient;
    private final FootballDataConfig footballDataConfig;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final GameweekRepository gameweekRepository;
    private final MatchRepository matchRepository;

    /**
     * Fetch upcoming fixtures (next 14 days) for all competitions and store them.
     */
    @Transactional
    public int syncUpcomingFixtures() {
        int totalSynced = 0;

        for (var entry : footballDataConfig.getCompetitions().entrySet()) {
            String competitionCode = entry.getValue();
            try {
                League league = leagueRepository.findByApiCompetitionCode(competitionCode).orElse(null);
                if (league == null) {
                    log.warn("No league found for competition code: {}", competitionCode);
                    continue;
                }

                MatchesResponse response = footballDataClient.getAllMatches(competitionCode);
                if (response == null || response.getMatches() == null) {
                    log.warn("No matches returned for competition {}", competitionCode);
                    continue;
                }

                int total = response.getMatches().size();
                int saved = 0;
                int skippedFinished = 0;
                int skippedExisting = 0;

                for (FootballDataDtos.Match matchData : response.getMatches()) {
                    String mappedStatus = mapStatus(matchData.getStatus());
                    if ("FINISHED".equals(mappedStatus) || "LIVE".equals(mappedStatus)
                            || "CANCELLED".equals(mappedStatus)) {
                        skippedFinished++;
                        continue;
                    }
                    try {
                        if (saveFixture(matchData, league)) {
                            saved++;
                        } else {
                            skippedExisting++;
                        }
                    } catch (Exception e) {
                        log.error("Error saving match {} (id={}): {}",
                                matchData.getHomeTeam() != null ? matchData.getHomeTeam().getName() + " vs " + matchData.getAwayTeam().getName() : "unknown",
                                matchData.getId(), e.getMessage());
                    }
                }

                totalSynced += saved;
                log.info("Competition {}: {} total, {} finished/live, {} already existed, {} newly saved",
                        entry.getKey(), total, skippedFinished, skippedExisting, saved);
            } catch (Exception e) {
                log.error("Failed to sync fixtures for {}: {}", entry.getKey(), e.getMessage());
            }
        }

        log.info("Total fixtures synced: {}", totalSynced);
        return totalSynced;
    }

    /**
     * Save a single fixture from football-data.org match data.
     */
    @Transactional
    public boolean saveFixture(FootballDataDtos.Match data, League league) {
        Integer fixtureId = data.getId();

        if (matchRepository.findByApiFixtureId(fixtureId).isPresent()) {
            return false;
        }

        Team homeTeam = getOrCreateTeam(data.getHomeTeam(), league);
        Team awayTeam = getOrCreateTeam(data.getAwayTeam(), league);
        Gameweek gameweek = getOrCreateGameweek(data.getMatchday(), league);

        LocalDateTime kickoff = OffsetDateTime.parse(data.getUtcDate()).toLocalDateTime();
        String status = mapStatus(data.getStatus());

        Match match = Match.builder()
                .league(league)
                .gameweek(gameweek)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .apiFixtureId(fixtureId)
                .kickoffTime(kickoff)
                .status(status)
                .build();

        matchRepository.save(match);
        log.debug("Saved fixture: {} vs {} ({})", homeTeam.getName(), awayTeam.getName(), kickoff);
        return true;
    }

    private Team getOrCreateTeam(MatchTeam teamInfo, League league) {
        return teamRepository.findByApiTeamId(teamInfo.getId())
                .orElseGet(() -> {
                    Team team = Team.builder()
                            .name(teamInfo.getName())
                            .shortName(teamInfo.getTla())
                            .logoUrl(teamInfo.getCrest())
                            .apiTeamId(teamInfo.getId())
                            .league(league)
                            .build();
                    return teamRepository.save(team);
                });
    }

    private Gameweek getOrCreateGameweek(Integer matchday, League league) {
        if (matchday == null) return null;

        return gameweekRepository.findByLeagueIdAndRoundNumber(league.getId(), matchday)
                .orElseGet(() -> {
                    Gameweek gw = Gameweek.builder()
                            .league(league)
                            .roundNumber(matchday)
                            .label("Matchday " + matchday)
                            .build();
                    return gameweekRepository.save(gw);
                });
    }

    private String mapStatus(String apiStatus) {
        if (apiStatus == null) return "SCHEDULED";
        return switch (apiStatus) {
            case "SCHEDULED", "TIMED" -> "SCHEDULED";
            case "IN_PLAY", "PAUSED" -> "LIVE";
            case "FINISHED" -> "FINISHED";
            case "POSTPONED" -> "POSTPONED";
            case "CANCELLED" -> "CANCELLED";
            default -> "SCHEDULED";
        };
    }
}
