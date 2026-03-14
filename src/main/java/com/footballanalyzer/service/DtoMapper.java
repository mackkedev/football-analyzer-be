package com.footballanalyzer.service;

import com.footballanalyzer.model.dto.Dtos.*;
import com.footballanalyzer.model.entity.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DtoMapper {

    public LeagueDto toLeagueDto(League league) {
        return LeagueDto.builder()
                .id(league.getId())
                .name(league.getName())
                .country(league.getCountry())
                .currentSeason(league.getCurrentSeason())
                .logoUrl(league.getLogoUrl())
                .build();
    }

    public TeamDto toTeamDto(Team team) {
        return TeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .shortName(team.getShortName())
                .logoUrl(team.getLogoUrl())
                .build();
    }

    public MatchDto toMatchDto(Match match) {
        MatchDto dto = MatchDto.builder()
                .id(match.getId())
                .league(toLeagueDto(match.getLeague()))
                .homeTeam(toTeamDto(match.getHomeTeam()))
                .awayTeam(toTeamDto(match.getAwayTeam()))
                .kickoffTime(match.getKickoffTime())
                .status(match.getStatus())
                .build();

        if (match.getGameweek() != null) {
            dto.setGameweekRound(match.getGameweek().getRoundNumber());
        }
        if (match.getResult() != null) {
            dto.setResult(toMatchResultDto(match.getResult()));
        }
        if (match.getAnalysis() != null) {
            dto.setAnalysis(toAnalysisSummaryDto(match.getAnalysis()));
        }
        return dto;
    }

    public MatchResultDto toMatchResultDto(MatchResult result) {
        return MatchResultDto.builder()
                .homeGoals1stHalf(result.getHomeGoals1stHalf())
                .awayGoals1stHalf(result.getAwayGoals1stHalf())
                .homeGoals2ndHalf(result.getHomeGoals2ndHalf())
                .awayGoals2ndHalf(result.getAwayGoals2ndHalf())
                .homeGoalsTotal(result.getHomeGoalsTotal())
                .awayGoalsTotal(result.getAwayGoalsTotal())
                .homeCorners(result.getHomeCorners())
                .awayCorners(result.getAwayCorners())
                .homeYellowCards(result.getHomeYellowCards())
                .awayYellowCards(result.getAwayYellowCards())
                .homeRedCards(result.getHomeRedCards())
                .awayRedCards(result.getAwayRedCards())
                .fullTimeResult(result.getFullTimeResult())
                .btts(result.getBtts())
                .moreGoals2ndHalf(result.getMoreGoals2ndHalf())
                .goalsTotal(result.getGoalsTotal())
                .build();
    }

    public AnalysisSummaryDto toAnalysisSummaryDto(Analysis analysis) {
        return AnalysisSummaryDto.builder()
                .resultPrediction(analysis.getResultPrediction())
                .resultConfidence(analysis.getResultConfidence())
                .bttsPrediction(analysis.getBttsPrediction())
                .bttsConfidence(analysis.getBttsConfidence())
                .moreGoals2ndHalfPrediction(analysis.getMoreGoals2ndHalfPrediction())
                .moreGoals2ndHalfConfidence(analysis.getMoreGoals2ndHalfConfidence())
                .mostShotsOnGoal(analysis.getMostShotsOnGoal())
                .mostYellowCards(analysis.getMostYellowCards())
                .mostCorners(analysis.getMostCorners())
                .build();
    }

    public AnalysisDetailDto toAnalysisDetailDto(Analysis analysis) {
        return AnalysisDetailDto.builder()
                .id(analysis.getId())
                .match(toMatchDto(analysis.getMatch()))
                .resultPrediction(analysis.getResultPrediction())
                .resultConfidence(analysis.getResultConfidence())
                .bttsPrediction(analysis.getBttsPrediction())
                .bttsConfidence(analysis.getBttsConfidence())
                .moreGoals2ndHalfPrediction(analysis.getMoreGoals2ndHalfPrediction())
                .moreGoals2ndHalfConfidence(analysis.getMoreGoals2ndHalfConfidence())
                .mostShotsOnGoal(analysis.getMostShotsOnGoal())
                .mostYellowCards(analysis.getMostYellowCards())
                .mostCorners(analysis.getMostCorners())
                .reasoning(analysis.getReasoning())
                .modelUsed(analysis.getModelUsed())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    public StryktipsetDrawDto toStryktipsetDrawDto(StryktipsetDraw draw) {
        return StryktipsetDrawDto.builder()
                .id(draw.getId())
                .drawNumber(draw.getDrawNumber())
                .drawState(draw.getDrawState())
                .regCloseTime(draw.getRegCloseTime())
                .events(draw.getEvents().stream()
                        .map(this::toStryktipsetEventDto)
                        .toList())
                .build();
    }

    public StryktipsetEventDto toStryktipsetEventDto(StryktipsetEvent event) {
        return StryktipsetEventDto.builder()
                .id(event.getId())
                .eventNumber(event.getEventNumber())
                .homeTeam(event.getHomeTeam())
                .awayTeam(event.getAwayTeam())
                .leagueName(event.getLeagueName())
                .kickoffTime(event.getKickoffTime())
                .odds1(event.getOdds1())
                .oddsX(event.getOddsX())
                .odds2(event.getOdds2())
                .sf1(event.getSf1())
                .sfX(event.getSfX())
                .sf2(event.getSf2())
                .aiPrediction(event.getAiPrediction())
                .aiConfidence(event.getAiConfidence())
                .aiReasoning(event.getAiReasoning())
                .couponSigns(event.getCouponSigns() != null
                        ? Arrays.asList(event.getCouponSigns().split(","))
                        : null)
                .couponReasoning(event.getCouponReasoning())
                .actualResult(event.getActualResult())
                .homeGoals(event.getHomeGoals())
                .awayGoals(event.getAwayGoals())
                .build();
    }
}
