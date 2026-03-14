package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameweek_id")
    private Gameweek gameweek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "api_fixture_id", nullable = false, unique = true)
    private Integer apiFixtureId;

    @Column(name = "kickoff_time", nullable = false)
    private LocalDateTime kickoffTime;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SCHEDULED";

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MatchResult result;

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Analysis analysis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
