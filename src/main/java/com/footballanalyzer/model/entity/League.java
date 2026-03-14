package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "league")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String country;

    @Column(name = "api_league_id", nullable = false, unique = true)
    private Integer apiLeagueId;

    @Column(name = "current_season", nullable = false)
    private Integer currentSeason;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "api_competition_code", length = 5)
    private String apiCompetitionCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "league", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Team> teams = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
