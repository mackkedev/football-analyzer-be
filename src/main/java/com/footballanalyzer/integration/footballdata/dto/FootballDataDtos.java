package com.footballanalyzer.integration.footballdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

public class FootballDataDtos {

    // ==========================================
    // Matches (GET /v4/competitions/{code}/matches)
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchesResponse {
        private Integer count;
        private Competition competition;
        private List<Match> matches;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Match {
        private Integer id;
        private String utcDate;       // "2026-03-01T15:00:00Z"
        private String status;        // SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED, POSTPONED, CANCELLED
        private Integer matchday;
        private String stage;
        private MatchTeam homeTeam;
        private MatchTeam awayTeam;
        private Score score;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchTeam {
        private Integer id;
        private String name;
        private String shortName;
        private String tla;
        private String crest;         // logo URL
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private String winner;        // HOME_TEAM, AWAY_TEAM, DRAW, null
        private String duration;
        private HalfScore fullTime;
        private HalfScore halfTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HalfScore {
        private Integer home;
        private Integer away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {
        private Integer id;
        private String name;
        private String code;
    }

    // ==========================================
    // Standings (GET /v4/competitions/{code}/standings)
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingsResponse {
        private Competition competition;
        private List<StandingGroup> standings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingGroup {
        private String stage;
        private String type;          // TOTAL, HOME, AWAY
        private List<TableEntry> table;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TableEntry {
        private Integer position;
        private MatchTeam team;
        private Integer playedGames;
        private String form;          // "W,W,D,L,W"
        private Integer won;
        private Integer draw;
        private Integer lost;
        private Integer points;
        private Integer goalsFor;
        private Integer goalsAgainst;
        private Integer goalDifference;
    }

    // ==========================================
    // Single match with H2H (GET /v4/matches/{id})
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SingleMatchResponse {
        private Head2Head head2head;
        private Match match;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Head2Head {
        private Integer numberOfMatches;
        private Integer totalGoals;
        private TeamRecord homeTeam;
        private TeamRecord awayTeam;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamRecord {
        private Integer id;
        private Integer wins;
        private Integer draws;
        private Integer losses;
    }
}
