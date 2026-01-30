package com.app.intelligence.analysis.services;

import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.mappers.ProductMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Gatherers;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;
    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;

    private static final String TRENDING_KEY_PREFIX = "analytics:trending:daily:";
    private static final int TOP_N_TRENDING = 10;

    @Override
    public void trackProductView(Long productId) {
        var key = getCurrentDailyKey();
        try {
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), 1);
            redisTemplate.expire(key, Duration.ofDays(7));
            
            // Micrometer metric
            meterRegistry.counter("catalog.product.views", "productId", productId.toString()).increment();
            
        } catch (Exception e) {
            log.error("Failed to track view for product {}: {}", productId, e.getMessage());
        }
    }

    @Override
    public void trackSearch(String keyword, int resultCount) {
        try {
            // Micrometer metric for search intent
            meterRegistry.counter("catalog.search.queries", "keyword", keyword).increment();
            
            // Distribution of result counts to identify "dead ends"
            DistributionSummary.builder("catalog.search.results")
                    .tag("keyword", keyword)
                    .register(meterRegistry)
                    .record(resultCount);
                    
            if (resultCount == 0) {
                meterRegistry.counter("catalog.search.no_results", "keyword", keyword).increment();
            }

            // Persistence for Metabase-like dashboards
            jdbcTemplate.update("INSERT INTO activity_logs (activity_type, metadata) VALUES (?, ?::jsonb)",
                    "SEARCH", String.format("{\"keyword\": \"%s\", \"result_count\": %d}", keyword, resultCount));

        } catch (Exception e) {
            log.error("Failed to track search for {}: {}", keyword, e.getMessage());
        }
    }

    @Override
    public void trackAddToCart(Long productId, String itemCode, java.math.BigDecimal price) {
        try {
            // Micrometer metric for conversion tracking
            meterRegistry.counter("catalog.cart.additions", 
                "productId", productId.toString(), 
                "itemCode", itemCode).increment();
                
            // Track potential cart value
            DistributionSummary.builder("catalog.cart.value")
                    .baseUnit("INR")
                    .register(meterRegistry)
                    .record(price.doubleValue());
                    
            // Persistence
            jdbcTemplate.update("INSERT INTO activity_logs (activity_type, metadata) VALUES (?, ?::jsonb)",
                    "CART_ADD", String.format("{\"product_id\": %d, \"item_code\": \"%s\", \"price\": %s}", 
                        productId, itemCode, price.toString()));

        } catch (Exception e) {
            log.error("Failed to track cart addition for {}: {}", itemCode, e.getMessage());
        }
    }

    @Override
    public List<ProductDTO> getTrendingProducts() {
        var key = getCurrentDailyKey();
        try {
            var topProductIds = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, TOP_N_TRENDING - 1);

            if (topProductIds == null || topProductIds.isEmpty()) {
                return Collections.emptyList();
            }

            var ids = topProductIds.stream()
                    .map(Long::parseLong)
                    .toList();

            // Use Java Stream Gatherers to fetch products in batches of 5 to avoid large DB
            // queries
            List<Product> products = ids.stream()
                    .gather(Gatherers.windowFixed(5))
                    .map(chunk -> productRepo.findAllById(chunk))
                    .flatMap(List::stream)
                    .toList();

            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));

            List<ProductDTO> result = new ArrayList<>();
            for (var id : ids) {
                if (productMap.containsKey(id)) {
                    result.add(productMapper.productToProductDTO(productMap.get(id)));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to fetch trending products: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getCurrentDailyKey() {
        return TRENDING_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }
}
