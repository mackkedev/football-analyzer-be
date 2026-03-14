package com.footballanalyzer.repository;

import com.footballanalyzer.model.entity.StryktipsetDraw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StryktipsetDrawRepository extends JpaRepository<StryktipsetDraw, Long> {

    Optional<StryktipsetDraw> findByDrawNumber(Integer drawNumber);

    @Query("SELECT d FROM StryktipsetDraw d WHERE d.drawState = 'Open' ORDER BY d.drawNumber DESC")
    List<StryktipsetDraw> findOpenDraws();

    @Query("SELECT d FROM StryktipsetDraw d ORDER BY d.drawNumber DESC")
    List<StryktipsetDraw> findAllByOrderByDrawNumberDesc();

    default Optional<StryktipsetDraw> findLatestOpen() {
        List<StryktipsetDraw> open = findOpenDraws();
        return open.isEmpty() ? Optional.empty() : Optional.of(open.get(0));
    }
}
