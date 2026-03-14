package com.footballanalyzer.controller;

import com.footballanalyzer.model.dto.Dtos.*;
import com.footballanalyzer.model.entity.Analysis;
import com.footballanalyzer.model.entity.Match;
import com.footballanalyzer.repository.AnalysisRepository;
import com.footballanalyzer.repository.MatchRepository;
import com.footballanalyzer.service.AnalysisService;
import com.footballanalyzer.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRepository analysisRepository;
    private final MatchRepository matchRepository;
    private final AnalysisService analysisService;
    private final DtoMapper mapper;

    /**
     * Get all analyses for the upcoming weekend.
     */
    @GetMapping("/weekend")
    public ResponseEntity<List<AnalysisDetailDto>> getWeekendAnalyses(
            @RequestParam(required = false) Long leagueId) {

        LocalDateTime start = getWeekendStart();
        LocalDateTime end = getWeekendEnd();

        List<Analysis> analyses;
        if (leagueId != null) {
            analyses = analysisRepository.findWeekendAnalysesByLeague(start, end, leagueId);
        } else {
            analyses = analysisRepository.findWeekendAnalyses(start, end);
        }

        List<AnalysisDetailDto> dtos = analyses.stream()
                .map(mapper::toAnalysisDetailDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get analysis for a specific match.
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<AnalysisDetailDto> getAnalysisByMatch(@PathVariable Long matchId) {
        return analysisRepository.findByMatchId(matchId)
                .map(mapper::toAnalysisDetailDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Trigger AI analysis for a specific match (admin).
     */
    @PostMapping("/generate/{matchId}")
    public ResponseEntity<?> generateAnalysis(@PathVariable Long matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }

        Analysis analysis = analysisService.generateAnalysis(match);
        if (analysis == null) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate analysis"));
        }

        return ResponseEntity.ok(mapper.toAnalysisDetailDto(analysis));
    }

    /**
     * Trigger AI analysis for all upcoming weekend matches (admin).
     */
    @PostMapping("/generate/weekend")
    public ResponseEntity<Map<String, Integer>> generateWeekendAnalyses() {
        int count = analysisService.generateWeekendAnalyses();
        return ResponseEntity.ok(Map.of("analyzedMatches", count));
    }

    private LocalDateTime getWeekendStart() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        LocalDate friday;
        if (dayOfWeek < DayOfWeek.FRIDAY.getValue()) {
            friday = today.plusDays(DayOfWeek.FRIDAY.getValue() - dayOfWeek);
        } else {
            friday = today;
        }
        return friday.atTime(LocalTime.MIDNIGHT);
    }

    private LocalDateTime getWeekendEnd() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        LocalDate monday;
        if (dayOfWeek <= DayOfWeek.FRIDAY.getValue()) {
            monday = today.plusDays(8 - dayOfWeek);
        } else {
            monday = today.plusDays(8 - dayOfWeek);
        }
        return monday.atTime(LocalTime.of(23, 59));
    }
}
