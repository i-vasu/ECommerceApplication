package com.app.security.services;

import com.app.security.entities.User;
import com.app.security.entities.CustomerSegment;
import com.app.security.repositories.CustomerSegmentRepo;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise Segment Evaluation Service.
 * Dynamically assigns users to segments based on SpEL rules.
 * High-performance Java 25 implementation with ScopedValue optimization
 * potential.
 */
@Service
public class SegmentEvaluationService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(SegmentEvaluationService.class);

    private final CustomerSegmentRepo segmentRepo;
    private final ExpressionParser parser = new SpelExpressionParser();

    public SegmentEvaluationService(CustomerSegmentRepo segmentRepo) {
        this.segmentRepo = segmentRepo;
    }

    public List<CustomerSegment> evaluateSegments(User user) {
        List<CustomerSegment> allSegments = segmentRepo.findAll();

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("user", user);

        return allSegments.stream()
                .filter(segment -> {
                    try {
                        return Boolean.TRUE.equals(parser.parseExpression(segment.getRuleExpression())
                                .getValue(context, Boolean.class));
                    } catch (Exception e) {
                        log.error("Rule evaluation failed for segment {}: {}", segment.getName(), e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
