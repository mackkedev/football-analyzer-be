package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.TeamForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamFormRepository extends JpaRepository<TeamForm, Long> {
    Optional<TeamForm> findByTeamIdAndLeagueIdAndSeason(Long teamId, Long leagueId, Integer season);
}
