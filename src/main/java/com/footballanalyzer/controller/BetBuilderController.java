package com.footballanalyzer.controller;

import com.footballanalyzer.model.dto.Dtos.BetBuilderCouponResponse;
import com.footballanalyzer.model.dto.Dtos.BetBuilderRequest;
import com.footballanalyzer.service.BetBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bet-builder")
@RequiredArgsConstructor
public class BetBuilderController {

    private final BetBuilderService betBuilderService;

    /**
     * Analyze a selection of matches and return a 4-match bet builder coupon.
     *
     * Request body: { "matchIds": [1, 2, 3, ...] }  (8-10 recommended, min 4, max 15)
     *
     * Response: 4 AI-selected matches, each with up to 3 bet builder options (~10 odds each)
     * and the AI's recommended option per match.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeCoupon(@RequestBody BetBuilderRequest request) {
        log.info("Bet builder coupon request for {} matches",
                request.getMatchIds() != null ? request.getMatchIds().size() : 0);

        try {
            BetBuilderCouponResponse response = betBuilderService.analyzeCoupon(request.getMatchIds());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Bet builder analysis failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
