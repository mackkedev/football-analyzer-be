package com.footballanalyzer.integration.claude;

import com.footballanalyzer.model.entity.Match;
import com.footballanalyzer.model.entity.TeamForm;
import com.footballanalyzer.integration.footballdata.dto.FootballDataDtos.Head2Head;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    /**
     * Build the full match context string to send to the AI.
     */
    public String buildMatchContext(Match match, TeamForm homeForm, TeamForm awayForm, Head2Head headToHead) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== MATCH ANALYSIS REQUEST ===\n\n");

        sb.append(String.format("Match: %s vs %s\n", match.getHomeTeam().getName(), match.getAwayTeam().getName()));
        sb.append(String.format("League: %s\n", match.getLeague().getName()));
        sb.append(String.format("Kickoff: %s\n\n", match.getKickoffTime()));

        // Home team form
        sb.append(String.format("=== %s (HOME) ===\n", match.getHomeTeam().getName()));
        if (homeForm != null) {
            appendTeamForm(sb, homeForm);
        } else {
            sb.append("No form data available.\n");
        }
        sb.append("\n");

        // Away team form
        sb.append(String.format("=== %s (AWAY) ===\n", match.getAwayTeam().getName()));
        if (awayForm != null) {
            appendTeamForm(sb, awayForm);
        } else {
            sb.append("No form data available.\n");
        }
        sb.append("\n");

        // Head to head (aggregate from football-data.org)
        sb.append("=== HEAD TO HEAD ===\n");
        if (headToHead != null && headToHead.getNumberOfMatches() != null && headToHead.getNumberOfMatches() > 0) {
            sb.append(String.format("Total meetings: %d | Total goals: %d\n",
                    headToHead.getNumberOfMatches(),
                    headToHead.getTotalGoals() != null ? headToHead.getTotalGoals() : 0));
            if (headToHead.getHomeTeam() != null) {
                sb.append(String.format("%s record: W%d D%d L%d\n",
                        match.getHomeTeam().getName(),
                        orZero(headToHead.getHomeTeam().getWins()),
                        orZero(headToHead.getHomeTeam().getDraws()),
                        orZero(headToHead.getHomeTeam().getLosses())));
            }
            if (headToHead.getAwayTeam() != null) {
                sb.append(String.format("%s record: W%d D%d L%d\n",
                        match.getAwayTeam().getName(),
                        orZero(headToHead.getAwayTeam().getWins()),
                        orZero(headToHead.getAwayTeam().getDraws()),
                        orZero(headToHead.getAwayTeam().getLosses())));
            }
        } else {
            sb.append("No H2H data available.\n");
        }

        return sb.toString();
    }

    /**
     * Build context for multiple matches to be analyzed together as a bet builder coupon.
     */
    public String buildBetBuilderContext(
            List<Match> matches,
            Map<Long, TeamForm> homeFormMap,
            Map<Long, TeamForm> awayFormMap,
            Map<Long, Head2Head> h2hMap) {

        StringBuilder sb = new StringBuilder();
        sb.append("=== BET BUILDER COUPON ANALYSIS ===\n");
        sb.append("You are analyzing ").append(matches.size()).append(" matches. ");
        sb.append("Select the best 4 and provide bet builder options for each.\n\n");

        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            sb.append("--- MATCH ").append(i + 1).append(" (ID: ").append(match.getId()).append(") ---\n");
            sb.append(String.format("Match: %s vs %s\n", match.getHomeTeam().getName(), match.getAwayTeam().getName()));
            sb.append(String.format("League: %s\n", match.getLeague().getName()));
            sb.append(String.format("Kickoff: %s\n\n", match.getKickoffTime()));

            TeamForm homeForm = homeFormMap.get(match.getId());
            TeamForm awayForm = awayFormMap.get(match.getId());

            sb.append(String.format("= %s (HOME) =\n", match.getHomeTeam().getName()));
            if (homeForm != null) appendTeamForm(sb, homeForm);
            else sb.append("No form data available.\n");
            sb.append("\n");

            sb.append(String.format("= %s (AWAY) =\n", match.getAwayTeam().getName()));
            if (awayForm != null) appendTeamForm(sb, awayForm);
            else sb.append("No form data available.\n");
            sb.append("\n");

            Head2Head h2h = h2hMap != null ? h2hMap.get(match.getId()) : null;
            sb.append("= HEAD TO HEAD =\n");
            if (h2h != null && h2h.getNumberOfMatches() != null && h2h.getNumberOfMatches() > 0) {
                sb.append(String.format("Meetings: %d | Goals: %d\n",
                        h2h.getNumberOfMatches(),
                        h2h.getTotalGoals() != null ? h2h.getTotalGoals() : 0));
                if (h2h.getHomeTeam() != null) {
                    sb.append(String.format("%s: W%d D%d L%d\n",
                            match.getHomeTeam().getName(),
                            orZero(h2h.getHomeTeam().getWins()),
                            orZero(h2h.getHomeTeam().getDraws()),
                            orZero(h2h.getHomeTeam().getLosses())));
                }
            } else {
                sb.append("No H2H data available.\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private void appendTeamForm(StringBuilder sb, TeamForm form) {
        sb.append(String.format("League Position: %d\n", form.getLeaguePosition()));
        sb.append(String.format("Form (last 5): %s\n", form.getLast5Form()));
        sb.append(String.format("Record: P%d W%d D%d L%d\n",
                form.getMatchesPlayed(), form.getWins(), form.getDraws(), form.getLosses()));
        sb.append(String.format("Home Record: %s\n", form.getHomeRecord()));
        sb.append(String.format("Away Record: %s\n", form.getAwayRecord()));
        sb.append(String.format("Goals: Scored %d (avg %.2f) | Conceded %d (avg %.2f)\n",
                form.getGoalsScored(), orZeroDecimal(form.getAvgGoalsScored()),
                form.getGoalsConceded(), orZeroDecimal(form.getAvgGoalsConceded())));
        sb.append(String.format("BTTS: %.0f%%\n", orZeroDecimal(form.getBttsPercentage())));
        sb.append(String.format("Over 2.5: %.0f%%\n", orZeroDecimal(form.getOver25Percentage())));
        sb.append(String.format("More goals 2nd half: %.0f%%\n", orZeroDecimal(form.getMoreGoals2ndHalfPct())));
        // Note: corners and cards stats may be zero if not tracked by the data provider
        if (isNonZero(form.getAvgCornersFor()) || isNonZero(form.getAvgCornersAgainst())) {
            sb.append(String.format("Corners: For avg %.1f | Against avg %.1f\n",
                    orZeroDecimal(form.getAvgCornersFor()), orZeroDecimal(form.getAvgCornersAgainst())));
        }
        if (isNonZero(form.getAvgYellowCards()) || isNonZero(form.getAvgRedCards())) {
            sb.append(String.format("Cards: Yellow avg %.1f | Red avg %.2f\n",
                    orZeroDecimal(form.getAvgYellowCards()), orZeroDecimal(form.getAvgRedCards())));
        }
    }

    private int orZero(Integer value) {
        return value != null ? value : 0;
    }

    private double orZeroDecimal(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private boolean isNonZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }
}
