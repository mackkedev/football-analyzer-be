package com.footballanalyzer.controller;

import com.footballanalyzer.model.dto.Dtos.StryktipsetDrawDto;
import com.footballanalyzer.repository.StryktipsetDrawRepository;
import com.footballanalyzer.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stryktipset")
@RequiredArgsConstructor
public class StryktipsetController {

    private final StryktipsetDrawRepository drawRepository;
    private final DtoMapper mapper;

    @GetMapping("/current")
    public ResponseEntity<StryktipsetDrawDto> getCurrentDraw() {
        return drawRepository.findLatestOpen()
                .or(() -> {
                    var all = drawRepository.findAllByOrderByDrawNumberDesc();
                    return all.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(all.get(0));
                })
                .map(mapper::toStryktipsetDrawDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/draw/{drawNumber}")
    public ResponseEntity<StryktipsetDrawDto> getDrawByNumber(@PathVariable Integer drawNumber) {
        return drawRepository.findByDrawNumber(drawNumber)
                .map(mapper::toStryktipsetDrawDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<StryktipsetDrawDto>> getHistory() {
        List<StryktipsetDrawDto> draws = drawRepository.findAllByOrderByDrawNumberDesc().stream()
                .map(mapper::toStryktipsetDrawDto)
                .toList();
        return ResponseEntity.ok(draws);
    }
}
