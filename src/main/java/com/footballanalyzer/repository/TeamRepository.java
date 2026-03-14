package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByApiTeamId(Integer apiTeamId);
    List<Team> findByLeagueId(Long leagueId);
}
