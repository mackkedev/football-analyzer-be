package com.footballanalyzer.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stryktipset_draw")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StryktipsetDraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draw_number", nullable = false, unique = true)
    private Integer drawNumber;

    @Column(name = "draw_state", nullable = false, length = 20)
    private String drawState; // "Open", "Finalized", etc.

    @Column(name = "reg_close_time", nullable = false)
    private LocalDateTime regCloseTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "draw", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("eventNumber ASC")
    @Builder.Default
    private List<StryktipsetEvent> events = new ArrayList<>();

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
