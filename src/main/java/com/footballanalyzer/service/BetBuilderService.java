package com.footballanalyzer.service;

import com.footballanalyzer.config.FootballDataConfig;
import com.footballanalyzer.integration.claude.AIClient;
import com.footballanalyzer.integration.claude.ClaudeAIClient.BetBuilderMatchPick;
import com.footballanalyzer.integration.claude.ClaudeAIClient.BetBuilderOptionAI;
import com.footballanalyzer.integration.claude.ClaudeAIClient.BetBuilderResponse;
import com.footballanalyzer.integration.claude.PromptBuilder;
import com.footballanalyzer.integration.footballdata.FootballDataClient;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.Head2Head;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.SingleMatchResponse;
import com.footballanalyzer.model.dto.Dtos.BetBuilderCouponResponse;
import com.footballanalyzer.model.dto.Dtos.BetBuilderMatchDto;
import com.footballanalyzer.model.dto.Dtos.BetBuilderOptionDto;
import com.footballanalyzer.model.entity.Match;
import com.footballanalyzer.model.entity.TeamForm;
import com.footballanalyzer.repository.MatchRepository;
import com.footballanalyzer.repository.TeamFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BetBuilderService {

    private final AIClient aiClient;
    private final MatchRepository matchRepository;
    private final TeamFormRepository teamFormRepository;
    private final FootballDataClient footballDataClient;
    private final FootballDataConfig footballDataConfig;
    private final PromptBuilder promptBuilder;
    private final DtoMapper dtoMapper;

    /**
     * Analyze the submitted match IDs with the AI and return a 4-match bet builder coupon.
     * The AI selects the best 4 matches from the provided list and suggests up to 3 bet
     * builder options per match (~10 odds each).
     */
    public BetBuilderCouponResponse analyzeCoupon(List<Long> matchIds) {
        if (matchIds == null || matchIds.size() < 4) {
            throw new IllegalArgumentException("At least 4 match IDs are required");
        }
        if (matchIds.size() > 15) {
            throw new IllegalArgumentException("Maximum 15 match IDs allowed");
        }

        List<Match> matches = matchRepository.findAllById(matchIds);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No matches found for the provided IDs");
        }

        log.info("Analyzing {} matches for bet builder coupon", matches.size());

        int season = footballDataConfig.getCurrentSeason();
        Map<Long, TeamForm> homeFormMap = new HashMap<>();
        Map<Long, TeamForm> awayFormMap = new HashMap<>();
        Map<Long, Head2Head> h2hMap = new HashMap<>();

        for (Match match : matches) {
            teamFormRepository.findByTeamIdAndLeagueIdAndSeason(
                    match.getHomeTeam().getId(), match.getLeague().getId(), season)
                    .ifPresent(f -> homeFormMap.put(match.getId(), f));

            teamFormRepository.findByTeamIdAndLeagueIdAndSeason(
                    match.getAwayTeam().getId(), match.getLeague().getId(), season)
                    .ifPresent(f -> awayFormMap.put(match.getId(), f));

            try {
                SingleMatchResponse matchResponse = footballDataClient.getMatch(match.getApiFixtureId());
                if (matchResponse != null && matchResponse.getHead2head() != null) {
                    h2hMap.put(match.getId(), matchResponse.getHead2head());
                }
            } catch (Exception e) {
                log.warn("Could not fetch H2H for match {}: {}", match.getId(), e.getMessage());
            }
        }

        String context = promptBuilder.buildBetBuilderContext(matches, homeFormMap, awayFormMap, h2hMap);
        BetBuilderResponse aiResponse = aiClient.analyzeBetBuilder(context);

        if (aiResponse == null || aiResponse.getSelectedMatches() == null) {
            log.error("AI returned no response for bet builder coupon");
            throw new RuntimeException("AI failed to generate bet builder coupon");
        }

        return mapToDto(aiResponse, matches);
    }

    private BetBuilderCouponResponse mapToDto(BetBuilderResponse aiResponse, List<Match> matches) {
        Map<Long, Match> matchById = new HashMap<>();
        for (Match m : matches) matchById.put(m.getId(), m);

        List<BetBuilderMatchDto> selectedMatches = new ArrayList<>();

        for (BetBuilderMatchPick pick : aiResponse.getSelectedMatches()) {
            Match match = matchById.get(pick.getMatchId());
            if (match == null) {
                log.warn("AI returned unknown match_id {}, skipping", pick.getMatchId());
                continue;
            }

            List<BetBuilderOptionDto> optionDtos = new ArrayList<>();
            if (pick.getBetOptions() != null) {
                for (BetBuilderOptionAI opt : pick.getBetOptions()) {
                    optionDtos.add(BetBuilderOptionDto.builder()
                            .description(opt.getDescription())
                            .selections(opt.getSelections())
                            .estimatedOdds(opt.getEstimatedOdds())
                            .confidence(opt.getConfidence())
                            .reasoning(opt.getReasoning())
                            .build());
                }
            }

            selectedMatches.add(BetBuilderMatchDto.builder()
                    .matchId(match.getId())
                    .homeTeam(dtoMapper.toTeamDto(match.getHomeTeam()))
                    .awayTeam(dtoMapper.toTeamDto(match.getAwayTeam()))
                    .league(dtoMapper.toLeagueDto(match.getLeague()))
                    .kickoffTime(match.getKickoffTime())
                    .betOptions(optionDtos)
                    .recommendedOption(pick.getRecommendedOption())
                    .build());
        }

        return BetBuilderCouponResponse.builder()
                .selectedMatches(selectedMatches)
                .overallReasoning(aiResponse.getOverallReasoning())
                .modelUsed(aiResponse.getModel())
                .build();
    }
}
