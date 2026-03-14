package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.PredictionAccuracy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionAccuracyRepository extends JpaRepository<PredictionAccuracy, Long> {

    Optional<PredictionAccuracy> findByMatchId(Long matchId);

    @Query("SELECT pa FROM PredictionAccuracy pa " +
           "JOIN pa.match m " +
           "WHERE m.gameweek.id = :gameweekId")
    List<PredictionAccuracy> findByGameweekId(@Param("gameweekId") Long gameweekId);

    @Query("SELECT pa FROM PredictionAccuracy pa " +
           "JOIN pa.match m " +
           "WHERE m.league.id = :leagueId")
    List<PredictionAccuracy> findByLeagueId(@Param("leagueId") Long leagueId);

    @Query("SELECT AVG(CASE WHEN pa.resultCorrect = true THEN 1.0 ELSE 0.0 END), " +
           "AVG(CASE WHEN pa.bttsCorrect = true THEN 1.0 ELSE 0.0 END), " +
           "AVG(CASE WHEN pa.moreGoals2ndHalfCorrect = true THEN 1.0 ELSE 0.0 END), " +
           "AVG(CASE WHEN pa.yellowCardsCorrect = true THEN 1.0 ELSE 0.0 END), " +
           "AVG(CASE WHEN pa.cornersCorrect = true THEN 1.0 ELSE 0.0 END), " +
           "COUNT(pa) " +
           "FROM PredictionAccuracy pa " +
           "JOIN pa.match m " +
           "WHERE (:leagueId IS NULL OR m.league.id = :leagueId)")
    Object[] getAccuracyOverview(@Param("leagueId") Long leagueId);
}
