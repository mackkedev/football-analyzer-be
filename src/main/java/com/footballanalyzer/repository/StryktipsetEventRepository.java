package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.StryktipsetEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StryktipsetEventRepository extends JpaRepository<StryktipsetEvent, Long> {

    List<StryktipsetEvent> findByDrawIdOrderByEventNumberAsc(Long drawId);

    @Query("SELECT e FROM StryktipsetEvent e WHERE e.draw.id = :drawId AND e.aiPrediction IS NULL ORDER BY e.eventNumber ASC")
    List<StryktipsetEvent> findByDrawIdWithoutPrediction(@Param("drawId") Long drawId);

    @Query("SELECT e FROM StryktipsetEvent e WHERE e.draw.id = :drawId AND e.couponSigns IS NULL ORDER BY e.eventNumber ASC")
    List<StryktipsetEvent> findByDrawIdWithoutCoupon(@Param("drawId") Long drawId);
}
