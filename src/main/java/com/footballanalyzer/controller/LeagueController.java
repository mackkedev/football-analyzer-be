package com.footballanalyzer.controller;

import com.footballanalyzer.model.dto.Dtos.LeagueDto;
import com.footballanalyzer.repository.LeagueRepository;
import com.footballanalyzer.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueRepository leagueRepository;
    private final DtoMapper mapper;

    @GetMapping
    public ResponseEntity<List<LeagueDto>> getAllLeagues() {
        List<LeagueDto> leagues = leagueRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toLeagueDto)
                .toList();
        return ResponseEntity.ok(leagues);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeagueDto> getLeague(@PathVariable Long id) {
        return leagueRepository.findById(id)
                .map(mapper::toLeagueDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
