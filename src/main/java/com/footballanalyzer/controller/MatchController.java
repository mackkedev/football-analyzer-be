package com.footballanalyzer.controller;

import com.footballanalyzer.model.dto.Dtos.*;
import com.footballanalyzer.model.entity.Match;
import com.footballanalyzer.repository.MatchRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchRepository matchRepository;
    private final DtoMapper mapper;

    /**
     * Get all matches for the upcoming weekend, grouped by league.
     */
    @GetMapping("/weekend")
    public ResponseEntity<WeekendOverviewDto> getWeekendMatches(
            @RequestParam(required = false) Long leagueId) {

        LocalDateTime start = getWeekendStart();
        LocalDateTime end = getWeekendEnd();

        List<Match> matches;
        if (leagueId != null) {
            matches = matchRepository.findWeekendMatchesByLeague(start, end, leagueId);
        } else {
            matches = matchRepository.findWeekendMatches(start, end);
        }

        // Group by league
        Map<Long, List<Match>> grouped = matches.stream()
                .collect(Collectors.groupingBy(m -> m.getLeague().getId()));

        List<LeagueMatchesDto> leagueMatches = grouped.entrySet().stream()
                .map(entry -> {
                    Match first = entry.getValue().get(0);
                    return LeagueMatchesDto.builder()
                            .league(mapper.toLeagueDto(first.getLeague()))
                            .gameweekRound(first.getGameweek() != null ? first.getGameweek().getRoundNumber() : null)
                            .matches(entry.getValue().stream().map(mapper::toMatchDto).toList())
                            .build();
                })
                .toList();

        String label = String.format("%s - %s", start.toLocalDate(), end.toLocalDate());

        WeekendOverviewDto overview = WeekendOverviewDto.builder()
                .weekendLabel(label)
                .leagueMatches(leagueMatches)
                .build();

        return ResponseEntity.ok(overview);
    }

    /**
     * Get a single match by ID with full details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatch(@PathVariable Long id) {
        return matchRepository.findById(id)
                .map(mapper::toMatchDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all matches for a specific gameweek.
     */
    @GetMapping("/gameweek/{gameweekId}")
    public ResponseEntity<List<MatchDto>> getMatchesByGameweek(@PathVariable Long gameweekId) {
        List<MatchDto> matches = matchRepository.findByGameweekId(gameweekId).stream()
                .map(mapper::toMatchDto)
                .toList();
        return ResponseEntity.ok(matches);
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
