package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByApiFixtureId(Integer apiFixtureId);

    @Query("SELECT m FROM Match m " +
           "JOIN FETCH m.homeTeam " +
           "JOIN FETCH m.awayTeam " +
           "JOIN FETCH m.league " +
           "WHERE m.kickoffTime BETWEEN :start AND :end " +
           "ORDER BY m.league.name, m.kickoffTime")
    List<Match> findWeekendMatches(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT m FROM Match m " +
           "JOIN FETCH m.homeTeam " +
           "JOIN FETCH m.awayTeam " +
           "JOIN FETCH m.league " +
           "WHERE m.kickoffTime BETWEEN :start AND :end " +
           "AND m.league.id = :leagueId " +
           "ORDER BY m.kickoffTime")
    List<Match> findWeekendMatchesByLeague(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("leagueId") Long leagueId);

    List<Match> findByGameweekId(Long gameweekId);

    @Query("SELECT m FROM Match m " +
           "JOIN FETCH m.homeTeam " +
           "JOIN FETCH m.awayTeam " +
           "WHERE m.status = 'FINISHED' " +
           "AND m.result IS NULL " +
           "ORDER BY m.kickoffTime")
    List<Match> findFinishedWithoutResult();

    @Query("SELECT m FROM Match m " +
           "WHERE m.analysis IS NULL " +
           "AND m.kickoffTime BETWEEN :start AND :end")
    List<Match> findScheduledWithoutAnalysis(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT m FROM Match m " +
           "WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) " +
           "AND m.status = 'FINISHED' " +
           "AND m.league.id = :leagueId " +
           "ORDER BY m.kickoffTime DESC")
    List<Match> findRecentMatchesByTeam(
            @Param("teamId") Long teamId,
            @Param("leagueId") Long leagueId);
}
