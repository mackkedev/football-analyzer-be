package com.footballanalyzer.integration.apifootball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTOs mapping API-Football v3 response structures.
 */
public class ApiFootballDtos {

    // ==========================================
    // Generic wrapper
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResponse<T> {
        private String get;
        private Map<String, String> parameters;
        private Object errors;
        private Integer results;
        private Paging paging;
        private List<T> response;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        private Integer current;
        private Integer total;
    }

    // ==========================================
    // Fixtures
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixtureData {
        private Fixture fixture;
        private LeagueInfo league;
        private Teams teams;
        private Goals goals;
        private Score score;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private Integer id;
        private String referee;
        private String timezone;
        private String date;
        private Long timestamp;
        private Venue venue;
        private Status status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Venue {
        private Integer id;
        private String name;
        private String city;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        @JsonProperty("long")
        private String longStatus;
        @JsonProperty("short")
        private String shortStatus;
        private Integer elapsed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueInfo {
        private Integer id;
        private String name;
        private String country;
        private String logo;
        private String flag;
        private Integer season;
        private String round;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Teams {
        private TeamInfo home;
        private TeamInfo away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamInfo {
        private Integer id;
        private String name;
        private String logo;
        private Boolean winner;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goals {
        private Integer home;
        private Integer away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        private Goals halftime;
        private Goals fulltime;
        private Goals extratime;
        private Goals penalty;
    }

    // ==========================================
    // Fixture Statistics
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixtureStatisticsData {
        private TeamInfo team;
        private List<StatisticItem> statistics;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatisticItem {
        private String type;
        private Object value; // Can be Integer, String, or null
    }

    // ==========================================
    // Fixture Events (goals, cards, etc.)
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventData {
        private EventTime time;
        private TeamInfo team;
        private Player player;
        private Player assist;
        private String type;     // "Goal", "Card", "subst"
        private String detail;   // "Normal Goal", "Yellow Card", "Red Card"
        private String comments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventTime {
        private Integer elapsed;
        private Integer extra;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        private Integer id;
        private String name;
    }

    // ==========================================
    // Standings
    // ==========================================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingsData {
        private LeagueStandings league;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueStandings {
        private Integer id;
        private String name;
        private String country;
        private Integer season;
        private List<List<StandingEntry>> standings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingEntry {
        private Integer rank;
        private TeamInfo team;
        private Integer points;
        private Integer goalsDiff;
        private String group;
        private String form;
        private String status;
        private String description;
        private StandingStats all;
        private StandingStats home;
        private StandingStats away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingStats {
        private Integer played;
        private Integer win;
        private Integer draw;
        private Integer lose;
        private StandingGoals goals;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingGoals {
        @JsonProperty("for")
        private Integer goalsFor;
        private Integer against;
    }

    // ==========================================
    // Head to Head
    // ==========================================
    // H2H returns List<FixtureData>, same structure as fixtures
}
