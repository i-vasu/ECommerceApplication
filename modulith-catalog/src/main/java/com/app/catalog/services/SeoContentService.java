package com.app.catalog.services;

import com.app.governance.rules.RuleEngineService;
import com.app.catalog.entities.Product;
import lombok.RequiredArgsConstructor;
// import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic SEO Content Service.
 * Generates meta tags and product titles using SpEL templates.
 */
@Service
// @Log4j2
@RequiredArgsConstructor
public class SeoContentService {

    private final RuleEngineService ruleEngine;

    public String generateMetaDescription(Product product) {
        Map<String, Object> context = new HashMap<>();
        context.put("p", product);

        // Dynamic Template: Can be loaded from database
        String template = "'Buy ' + p.productName + ' from ' + p.brand + ' at just ₹' + (p.specialPrice ?: p.price) + '. Best quality ' + p.category.categoryName + '!'";

        return ruleEngine.evaluate(template, context, String.class);
    }
}
