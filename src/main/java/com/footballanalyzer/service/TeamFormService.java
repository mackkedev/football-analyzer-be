package com.footballanalyzer.service;

import com.footballanalyzer.config.FootballDataConfig;
import com.footballanalyzer.integration.footballdata.FootballDataClient;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.StandingsResponse;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.StandingGroup;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.TableEntry;
import com.footballanalyzer.model.entity.League;
import com.footballanalyzer.model.entity.Team;
import com.footballanalyzer.model.entity.TeamForm;
import com.footballanalyzer.repository.LeagueRepository;
import com.footballanalyzer.repository.TeamFormRepository;
import com.footballanalyzer.repository.TeamRepository;
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
public class TeamFormService {

    private final FootballDataClient footballDataClient;
    private final FootballDataConfig footballDataConfig;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final TeamFormRepository teamFormRepository;

    /**
     * Sync league standings → team_form for all competitions.
     * Populates position, W/D/L, goals, form string, home/away splits.
     */
    @Transactional
    public int syncStandings() {
        int totalSynced = 0;

        for (var entry : footballDataConfig.getCompetitions().entrySet()) {
            String competitionCode = entry.getValue();
            try {
                League league = leagueRepository.findByApiCompetitionCode(competitionCode).orElse(null);
                if (league == null) {
                    log.warn("No league found for competition code: {}", competitionCode);
                    continue;
                }

                StandingsResponse response = footballDataClient.getStandings(competitionCode);
                if (response == null || response.getStandings() == null) {
                    log.warn("No standings returned for competition {}", competitionCode);
                    continue;
                }

                List<TableEntry> totalTable = getTable(response, "TOTAL");
                List<TableEntry> homeTable  = getTable(response, "HOME");
                List<TableEntry> awayTable  = getTable(response, "AWAY");

                if (totalTable == null || totalTable.isEmpty()) {
                    log.warn("No TOTAL standings table for {}", competitionCode);
                    continue;
                }

                int season = footballDataConfig.getCurrentSeason();
                int saved = 0;

                for (TableEntry total : totalTable) {
                    try {
                        Team team = teamRepository.findByApiTeamId(total.getTeam().getId()).orElse(null);
                        if (team == null) {
                            log.debug("Team not found in DB for API id {}, skipping", total.getTeam().getId());
                            continue;
                        }

                        TableEntry home = findEntry(homeTable, total.getTeam().getId());
                        TableEntry away = findEntry(awayTable, total.getTeam().getId());

                        TeamForm form = teamFormRepository
                                .findByTeamIdAndLeagueIdAndSeason(team.getId(), league.getId(), season)
                                .orElse(TeamForm.builder().team(team).league(league).season(season).build());

                        int played     = orZero(total.getPlayedGames());
                        int goalsFor   = orZero(total.getGoalsFor());
                        int goalsAgainst = orZero(total.getGoalsAgainst());

                        form.setLeaguePosition(total.getPosition());
                        form.setMatchesPlayed(played);
                        form.setWins(orZero(total.getWon()));
                        form.setDraws(orZero(total.getDraw()));
                        form.setLosses(orZero(total.getLost()));
                        form.setGoalsScored(goalsFor);
                        form.setGoalsConceded(goalsAgainst);
                        form.setLast5Form(formatForm(total.getForm()));

                        if (played > 0) {
                            form.setAvgGoalsScored(bd(goalsFor).divide(bd(played), 2, RoundingMode.HALF_UP));
                            form.setAvgGoalsConceded(bd(goalsAgainst).divide(bd(played), 2, RoundingMode.HALF_UP));
                        }

                        if (home != null) {
                            form.setHomeWins(orZero(home.getWon()));
                            form.setHomeDraws(orZero(home.getDraw()));
                            form.setHomeLosses(orZero(home.getLost()));
                        }
                        if (away != null) {
                            form.setAwayWins(orZero(away.getWon()));
                            form.setAwayDraws(orZero(away.getDraw()));
                            form.setAwayLosses(orZero(away.getLost()));
                        }

                        teamFormRepository.save(form);
                        saved++;
                    } catch (Exception e) {
                        log.error("Error saving form for team {} (id={}): {}",
                                total.getTeam().getName(), total.getTeam().getId(), e.getMessage());
                    }
                }

                totalSynced += saved;
                log.info("Competition {}: {} team forms synced", entry.getKey(), saved);
            } catch (Exception e) {
                log.error("Failed to sync standings for {}: {}", entry.getKey(), e.getMessage());
            }
        }

        log.info("Total team forms synced: {}", totalSynced);
        return totalSynced;
    }

    private List<TableEntry> getTable(StandingsResponse response, String type) {
        return response.getStandings().stream()
                .filter(sg -> type.equals(sg.getType()))
                .findFirst()
                .map(StandingGroup::getTable)
                .orElse(null);
    }

    private TableEntry findEntry(List<TableEntry> table, Integer apiTeamId) {
        if (table == null) return null;
        return table.stream()
                .filter(e -> apiTeamId.equals(e.getTeam().getId()))
                .findFirst()
                .orElse(null);
    }

    /** API returns "W,W,D,L,W" → we want "WWDLW" (last 5). */
    private String formatForm(String form) {
        if (form == null) return null;
        String stripped = form.replace(",", "");
        return stripped.length() > 5 ? stripped.substring(stripped.length() - 5) : stripped;
    }

    private int orZero(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
