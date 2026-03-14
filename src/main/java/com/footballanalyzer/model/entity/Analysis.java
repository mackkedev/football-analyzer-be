package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    // --- Match Result Prediction ---
    @Column(name = "result_prediction", nullable = false, length = 3)
    private String resultPrediction; // "1", "X", "2", "1,X", "X,2", "1,2"

    @Column(name = "result_confidence", nullable = false)
    private BigDecimal resultConfidence;

    // --- BTTS Prediction ---
    @Column(name = "btts_prediction", nullable = false)
    private Boolean bttsPrediction;

    @Column(name = "btts_confidence", nullable = false)
    private BigDecimal bttsConfidence;

    // --- More Goals 2nd Half ---
    @Column(name = "more_goals_2nd_half_prediction", nullable = false)
    private Boolean moreGoals2ndHalfPrediction;

    @Column(name = "more_goals_2nd_half_confidence", nullable = false)
    private BigDecimal moreGoals2ndHalfConfidence;

    // --- Most Shots On Goal ---
    @Column(name = "most_shots_on_goal", length = 4)
    private String mostShotsOnGoal; // "HOME" or "AWAY"

    // --- Most Yellow Cards ---
    @Column(name = "most_yellow_cards", length = 4)
    private String mostYellowCards; // "HOME" or "AWAY"

    // --- Most Corners ---
    @Column(name = "most_corners", length = 4)
    private String mostCorners; // "HOME" or "AWAY"

    // --- AI Output ---
    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}
