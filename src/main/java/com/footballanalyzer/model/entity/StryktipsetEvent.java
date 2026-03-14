package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stryktipset_event", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"draw_id", "event_number"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StryktipsetEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_id", nullable = false)
    private StryktipsetDraw draw;

    @Column(name = "event_number", nullable = false)
    private Integer eventNumber; // 1-13

    @Column(name = "home_team", nullable = false, length = 100)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 100)
    private String awayTeam;

    @Column(name = "league_name", length = 100)
    private String leagueName;

    @Column(name = "kickoff_time")
    private LocalDateTime kickoffTime;

    // Odds from Svenska Spel
    @Column(name = "odds_1", precision = 6, scale = 2)
    private BigDecimal odds1;

    @Column(name = "odds_x", precision = 6, scale = 2)
    private BigDecimal oddsX;

    @Column(name = "odds_2", precision = 6, scale = 2)
    private BigDecimal odds2;

    // Svenska folket distribution (percentages)
    @Column(name = "sf_1", precision = 5, scale = 2)
    private BigDecimal sf1;

    @Column(name = "sf_x", precision = 5, scale = 2)
    private BigDecimal sfX;

    @Column(name = "sf_2", precision = 5, scale = 2)
    private BigDecimal sf2;

    // AI prediction
    @Column(name = "ai_prediction", length = 1)
    private String aiPrediction; // "1", "X", "2"

    @Column(name = "ai_confidence", precision = 3, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "ai_reasoning", columnDefinition = "TEXT")
    private String aiReasoning;

    // 128kr coupon signs — "1", "X", "2", "1,X", "X,2", or "1,2"
    @Column(name = "coupon_signs", length = 5)
    private String couponSigns;

    @Column(name = "coupon_reasoning", columnDefinition = "TEXT")
    private String couponReasoning;

    // Actual result
    @Column(name = "actual_result", length = 1)
    private String actualResult; // "1", "X", "2"

    @Column(name = "home_goals")
    private Integer homeGoals;

    @Column(name = "away_goals")
    private Integer awayGoals;
}
