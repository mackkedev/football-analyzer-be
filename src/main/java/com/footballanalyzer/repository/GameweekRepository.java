package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.Gameweek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameweekRepository extends JpaRepository<Gameweek, Long> {
    Optional<Gameweek> findByLeagueIdAndRoundNumber(Long leagueId, Integer roundNumber);
}
