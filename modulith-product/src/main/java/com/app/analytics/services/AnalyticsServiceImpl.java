package com.app.analytics.services;

import com.app.product.payloads.ProductDTO;
import com.app.product.entities.Product;
import com.app.product.repositories.ProductRepo;
import com.app.product.mappers.ProductMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    private static final String TRENDING_KEY_PREFIX = "analytics:trending:daily:";
    private static final int TOP_N_TRENDING = 10;

    @Override
    public void trackProductView(Long productId) {
        var key = getCurrentDailyKey();
        try {
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), 1);
            redisTemplate.expire(key, Duration.ofDays(7));
        } catch (Exception e) {
            log.error("Failed to track view for product {}: {}", productId, e.getMessage());
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
