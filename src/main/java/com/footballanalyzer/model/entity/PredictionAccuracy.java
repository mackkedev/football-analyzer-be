package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_accuracy")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PredictionAccuracy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, unique = true)
    private Analysis analysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "result_correct")
    private Boolean resultCorrect;

    @Column(name = "btts_correct")
    private Boolean bttsCorrect;

    @Column(name = "more_goals_2nd_half_correct")
    private Boolean moreGoals2ndHalfCorrect;

    @Column(name = "shots_correct")
    private Boolean shotsCorrect; // null if shots data unavailable

    @Column(name = "yellow_cards_correct")
    private Boolean yellowCardsCorrect;

    @Column(name = "corners_correct")
    private Boolean cornersCorrect;

    private Integer score; // out of 5 (shots excluded when null)

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the score based on correct predictions
     */
    public void calculateScore() {
        int s = 0;
        if (Boolean.TRUE.equals(resultCorrect)) s++;
        if (Boolean.TRUE.equals(bttsCorrect)) s++;
        if (Boolean.TRUE.equals(moreGoals2ndHalfCorrect)) s++;
        if (Boolean.TRUE.equals(yellowCardsCorrect)) s++;
        if (Boolean.TRUE.equals(cornersCorrect)) s++;
        // shotsCorrect excluded — no shots data available from free tier
        this.score = s;
    }
}
