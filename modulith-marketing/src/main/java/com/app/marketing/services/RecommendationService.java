package com.app.marketing.services;

import com.app.intelligence.analysis.services.AnalyticsService;
import com.app.catalog.payloads.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AnalyticsService analyticsService;
    private final com.app.governance.rules.RuleEngineService ruleEngine;

    public List<ProductDTO> getPersonalizedRecommendations(String email, double userLifetimeSpend) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("spend", userLifetimeSpend);

        // Strategy defined as SpEL: If spend > 10,000, prioritize Premium over Trending
        String strategy = "spend > 10000 ? 'PREMIUM' : 'TRENDING'";
        String result = ruleEngine.evaluate(strategy, context, String.class);

        if ("PREMIUM".equals(result)) {
            // Simulated premium filter logic
            return analyticsService.getTrendingProducts().stream().limit(2).collect(Collectors.toList());
        }

        return getTrendingProducts();
    }

    public List<ProductDTO> getTrendingProducts() {
        return analyticsService.getTrendingProducts().stream()
                .limit(6)
                .collect(Collectors.toList());
    }
}
