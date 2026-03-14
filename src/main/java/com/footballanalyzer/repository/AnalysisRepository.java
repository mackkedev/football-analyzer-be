package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByMatchId(Long matchId);

    @Query("SELECT a FROM Analysis a " +
           "JOIN FETCH a.match m " +
           "JOIN FETCH m.homeTeam " +
           "JOIN FETCH m.awayTeam " +
           "JOIN FETCH m.league " +
           "WHERE m.kickoffTime BETWEEN :start AND :end " +
           "ORDER BY m.league.name, m.kickoffTime")
    List<Analysis> findWeekendAnalyses(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Analysis a " +
           "JOIN FETCH a.match m " +
           "JOIN FETCH m.homeTeam " +
           "JOIN FETCH m.awayTeam " +
           "WHERE m.kickoffTime BETWEEN :start AND :end " +
           "AND m.league.id = :leagueId " +
           "ORDER BY m.kickoffTime")
    List<Analysis> findWeekendAnalysesByLeague(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("leagueId") Long leagueId);
}
