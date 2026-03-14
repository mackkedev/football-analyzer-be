package com.footballanalyzer.integration.svenskaspel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.List;

public class StryktipsetApiDtos {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrawsResponse {
        private List<Draw> draws;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Draw {
        @JsonProperty("drawNumber")
        private Integer drawNumber;

        @JsonProperty("drawState")
        private String drawState; // "Open", "Finalized", etc.

        @JsonProperty("regCloseTime")
        private String regCloseTime; // ISO datetime string

        @JsonProperty("drawEvents")
        private List<DrawEvent> drawEvents;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrawEvent {
        @JsonProperty("eventNumber")
        private Integer eventNumber;

        @JsonProperty("match")
        private MatchInfo match;

        @JsonProperty("odds")
        private Odds odds;

        @JsonProperty("svenskaFolket")
        private SvenskaFolket svenskaFolket;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchInfo {
        @JsonProperty("matchId")
        private Integer matchId;

        // Derived from the "participants" array in the API response
        private String homeTeamName;
        private String awayTeamName;

        // Derived from the nested "league" object in the API response
        private String leagueName;

        @JsonProperty("matchStart")
        private String matchStart; // ISO datetime string

        @JsonProperty("homeTeamResult")
        private Integer homeTeamResult;

        @JsonProperty("awayTeamResult")
        private Integer awayTeamResult;

        @JsonProperty("statusId")
        private Integer statusId;

        @JsonSetter("participants")
        public void setParticipantsFromJson(List<Participant> participants) {
            if (participants != null) {
                for (Participant p : participants) {
                    if ("home".equals(p.getType())) {
                        this.homeTeamName = p.getName();
                    } else if ("away".equals(p.getType())) {
                        this.awayTeamName = p.getName();
                    }
                }
            }
        }

        @JsonSetter("league")
        public void setLeagueFromJson(LeagueRef league) {
            if (league != null) {
                this.leagueName = league.getName();
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Participant {
        @JsonProperty("type")
        private String type; // "home" or "away"

        @JsonProperty("name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueRef {
        @JsonProperty("name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Odds {
        @JsonProperty("one")
        private String one; // e.g. "2,10" (Swedish decimal format)

        @JsonProperty("x")
        private String x;

        @JsonProperty("two")
        private String two;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SvenskaFolket {
        @JsonProperty("one")
        private String one; // e.g. "45" (percentage)

        @JsonProperty("x")
        private String x;

        @JsonProperty("two")
        private String two;
    }
}
