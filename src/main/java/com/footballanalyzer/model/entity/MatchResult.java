package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_result")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Column(name = "home_goals_1st_half")
    private Integer homeGoals1stHalf;

    @Column(name = "away_goals_1st_half")
    private Integer awayGoals1stHalf;

    @Column(name = "home_goals_2nd_half")
    private Integer homeGoals2ndHalf;

    @Column(name = "away_goals_2nd_half")
    private Integer awayGoals2ndHalf;

    @Column(name = "home_goals_total", nullable = false)
    private Integer homeGoalsTotal;

    @Column(name = "away_goals_total", nullable = false)
    private Integer awayGoalsTotal;

    @Column(name = "home_corners")
    private Integer homeCorners;

    @Column(name = "away_corners")
    private Integer awayCorners;

    @Column(name = "home_yellow_cards")
    private Integer homeYellowCards;

    @Column(name = "away_yellow_cards")
    private Integer awayYellowCards;

    @Column(name = "home_red_cards")
    private Integer homeRedCards;

    @Column(name = "away_red_cards")
    private Integer awayRedCards;

    @Column(name = "full_time_result", nullable = false, length = 1)
    private String fullTimeResult; // "1", "X", "2"

    @Column(nullable = false)
    private Boolean btts;

    @Column(name = "more_goals_2nd_half")
    private Boolean moreGoals2ndHalf;

    @Column(name = "goals_total", nullable = false)
    private BigDecimal goalsTotal;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Derives full_time_result from goals
     */
    public void calculateDerivedFields() {
        // Full time result
        if (homeGoalsTotal > awayGoalsTotal) {
            this.fullTimeResult = "1";
        } else if (homeGoalsTotal < awayGoalsTotal) {
            this.fullTimeResult = "2";
        } else {
            this.fullTimeResult = "X";
        }

        // BTTS
        this.btts = homeGoalsTotal > 0 && awayGoalsTotal > 0;

        // Total goals
        this.goalsTotal = BigDecimal.valueOf(homeGoalsTotal + awayGoalsTotal);

        // More goals 2nd half
        if (homeGoals1stHalf != null && awayGoals1stHalf != null
                && homeGoals2ndHalf != null && awayGoals2ndHalf != null) {
            int firstHalfGoals = homeGoals1stHalf + awayGoals1stHalf;
            int secondHalfGoals = homeGoals2ndHalf + awayGoals2ndHalf;
            this.moreGoals2ndHalf = secondHalfGoals > firstHalfGoals;
        }
    }
}
