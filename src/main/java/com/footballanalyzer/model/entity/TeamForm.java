package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_form", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "league_id", "season"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TeamForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(nullable = false)
    private Integer season;

    @Column(name = "matches_played", nullable = false)
    @Builder.Default
    private Integer matchesPlayed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer draws = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer losses = 0;

    @Column(name = "goals_scored", nullable = false)
    @Builder.Default
    private Integer goalsScored = 0;

    @Column(name = "goals_conceded", nullable = false)
    @Builder.Default
    private Integer goalsConceded = 0;

    @Column(name = "avg_goals_scored")
    private BigDecimal avgGoalsScored;

    @Column(name = "avg_goals_conceded")
    private BigDecimal avgGoalsConceded;

    @Column(name = "avg_corners_for")
    private BigDecimal avgCornersFor;

    @Column(name = "avg_corners_against")
    private BigDecimal avgCornersAgainst;

    @Column(name = "avg_yellow_cards")
    private BigDecimal avgYellowCards;

    @Column(name = "avg_red_cards")
    private BigDecimal avgRedCards;

    @Column(name = "btts_percentage")
    private BigDecimal bttsPercentage;

    @Column(name = "over_25_percentage")
    private BigDecimal over25Percentage;

    @Column(name = "more_goals_2nd_half_pct")
    private BigDecimal moreGoals2ndHalfPct;

    @Column(name = "home_wins")
    @Builder.Default
    private Integer homeWins = 0;

    @Column(name = "home_draws")
    @Builder.Default
    private Integer homeDraws = 0;

    @Column(name = "home_losses")
    @Builder.Default
    private Integer homeLosses = 0;

    @Column(name = "away_wins")
    @Builder.Default
    private Integer awayWins = 0;

    @Column(name = "away_draws")
    @Builder.Default
    private Integer awayDraws = 0;

    @Column(name = "away_losses")
    @Builder.Default
    private Integer awayLosses = 0;

    @Column(name = "last_5_form", length = 5)
    private String last5Form;

    @Column(name = "league_position")
    private Integer leaguePosition;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }

    public String getHomeRecord() {
        return String.format("W%d D%d L%d", homeWins, homeDraws, homeLosses);
    }

    public String getAwayRecord() {
        return String.format("W%d D%d L%d", awayWins, awayDraws, awayLosses);
    }
}
