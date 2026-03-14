package com.footballanalyzer.model.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Dtos {

    // ==========================================
    // League DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LeagueDto {
        private Long id;
        private String name;
        private String country;
        private Integer currentSeason;
        private String logoUrl;
    }

    // ==========================================
    // Team DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TeamDto {
        private Long id;
        private String name;
        private String shortName;
        private String logoUrl;
    }

    // ==========================================
    // Match DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MatchDto {
        private Long id;
        private LeagueDto league;
        private Integer gameweekRound;
        private TeamDto homeTeam;
        private TeamDto awayTeam;
        private LocalDateTime kickoffTime;
        private String status;
        private MatchResultDto result;
        private AnalysisSummaryDto analysis;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MatchResultDto {
        private Integer homeGoals1stHalf;
        private Integer awayGoals1stHalf;
        private Integer homeGoals2ndHalf;
        private Integer awayGoals2ndHalf;
        private Integer homeGoalsTotal;
        private Integer awayGoalsTotal;
        private Integer homeCorners;
        private Integer awayCorners;
        private Integer homeYellowCards;
        private Integer awayYellowCards;
        private Integer homeRedCards;
        private Integer awayRedCards;
        private String fullTimeResult;
        private Boolean btts;
        private Boolean moreGoals2ndHalf;
        private BigDecimal goalsTotal;
    }

    // ==========================================
    // Analysis DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AnalysisSummaryDto {
        private String resultPrediction;       // "1", "X", "2", "1,X", "X,2", "1,2"
        private BigDecimal resultConfidence;
        private Boolean bttsPrediction;
        private BigDecimal bttsConfidence;
        private Boolean moreGoals2ndHalfPrediction;
        private BigDecimal moreGoals2ndHalfConfidence;
        private String mostShotsOnGoal;        // "HOME" or "AWAY"
        private String mostYellowCards;        // "HOME" or "AWAY"
        private String mostCorners;            // "HOME" or "AWAY"
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AnalysisDetailDto {
        private Long id;
        private MatchDto match;
        private String resultPrediction;       // "1", "X", "2", "1,X", "X,2", "1,2"
        private BigDecimal resultConfidence;
        private Boolean bttsPrediction;
        private BigDecimal bttsConfidence;
        private Boolean moreGoals2ndHalfPrediction;
        private BigDecimal moreGoals2ndHalfConfidence;
        private String mostShotsOnGoal;        // "HOME" or "AWAY"
        private String mostYellowCards;        // "HOME" or "AWAY"
        private String mostCorners;            // "HOME" or "AWAY"
        private String reasoning;
        private String modelUsed;
        private LocalDateTime analyzedAt;
    }

    // ==========================================
    // Accuracy DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AccuracyDto {
        private Long matchId;
        private String homeTeam;
        private String awayTeam;
        private Boolean resultCorrect;
        private Boolean bttsCorrect;
        private Boolean moreGoals2ndHalfCorrect;
        private Boolean shotsCorrect;
        private Boolean yellowCardsCorrect;
        private Boolean cornersCorrect;
        private Integer score;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AccuracyOverviewDto {
        private String leagueName;
        private Integer totalMatches;
        private BigDecimal resultAccuracy;
        private BigDecimal bttsAccuracy;
        private BigDecimal moreGoals2ndHalfAccuracy;
        private BigDecimal yellowCardsAccuracy;
        private BigDecimal cornersAccuracy;
        private BigDecimal overallAccuracy;
    }

    // ==========================================
    // Weekend Overview
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WeekendOverviewDto {
        private String weekendLabel; // e.g. "Feb 28 - Mar 1, 2026"
        private java.util.List<LeagueMatchesDto> leagueMatches;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LeagueMatchesDto {
        private LeagueDto league;
        private Integer gameweekRound;
        private java.util.List<MatchDto> matches;
    }

    // ==========================================
    // Bet Builder DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BetBuilderRequest {
        private java.util.List<Long> matchIds;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BetBuilderOptionDto {
        private String description;
        private java.util.List<String> selections;
        private Double estimatedOdds;
        private Double confidence;
        private String reasoning;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BetBuilderMatchDto {
        private Long matchId;
        private TeamDto homeTeam;
        private TeamDto awayTeam;
        private LeagueDto league;
        private LocalDateTime kickoffTime;
        private java.util.List<BetBuilderOptionDto> betOptions;
        private Integer recommendedOption; // index into betOptions (0-based)
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BetBuilderCouponResponse {
        private java.util.List<BetBuilderMatchDto> selectedMatches; // 4 AI-chosen matches
        private String overallReasoning;
        private String modelUsed;
    }

    // ==========================================
    // Stryktipset DTOs
    // ==========================================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StryktipsetDrawDto {
        private Long id;
        private Integer drawNumber;
        private String drawState;
        private LocalDateTime regCloseTime;
        private java.util.List<StryktipsetEventDto> events;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StryktipsetEventDto {
        private Long id;
        private Integer eventNumber;
        private String homeTeam;
        private String awayTeam;
        private String leagueName;
        private LocalDateTime kickoffTime;
        private BigDecimal odds1;
        private BigDecimal oddsX;
        private BigDecimal odds2;
        private BigDecimal sf1;
        private BigDecimal sfX;
        private BigDecimal sf2;
        private String aiPrediction;
        private BigDecimal aiConfidence;
        private String aiReasoning;
        private java.util.List<String> couponSigns; // e.g. ["1"], ["1","X"], ["X","2"], ["1","2"]
        private String couponReasoning;
        private String actualResult;
        private Integer homeGoals;
        private Integer awayGoals;
    }
}
