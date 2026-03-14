package com.footballanalyzer.integration.claude;

public interface AIClient {
    ClaudeAIClient.AnalysisResponse analyzeMatch(String matchContext);
    ClaudeAIClient.StryktipsetPredictionsResponse analyzeStryktipset(String matchContext);
    ClaudeAIClient.StryktipsetCouponResponse generateStryktipsetCoupon(String matchContext);
    ClaudeAIClient.BetBuilderResponse analyzeBetBuilder(String matchContext);
}
