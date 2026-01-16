package com.app.search;

import com.app.payloads.ProductDTO;
import com.app.entites.Product;
import com.app.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final StringRedisTemplate redisTemplate;
    private final ProductRepo productRepo;
    private final ModelMapper modelMapper;

    private static final String TRENDING_KEY_PREFIX = "analytics:trending:daily:";
    private static final int TOP_N_TRENDING = 10;

    @Override
    public void trackProductView(Long productId) {
        String key = getCurrentDailyKey();
        try {
            // Increment score in Sorted Set (ZSET)
            // ZINCRBY analytics:trending:daily:2026-01-15 1 123
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), 1);

            // Set expiry to 7 days to auto-cleanup old data
            redisTemplate.expire(key, java.time.Duration.ofDays(7));
        } catch (Exception e) {
            log.error("Failed to track view for product {}", productId, e);
        }
    }

    @Override
    public List<ProductDTO> getTrendingProducts() {
        String key = getCurrentDailyKey();
        try {
            // Get top N product IDs with highest scores
            // ZREVRANGE analytics:trending:daily:2026-01-15 0 9
            Set<String> topProductIds = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, TOP_N_TRENDING - 1);

            if (topProductIds == null || topProductIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> ids = topProductIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // Fetch product details from DB (or Cache via Repository/Service)
            // Ideally we should use ProductService to hit cache, but Repo is fine for now
            List<Product> products = productRepo.findAllById(ids);

            // Maintain the order of trending (findAllById doesn't guarantee order)
            // We map ID to Product for O(1) lookup
            java.util.Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));

            List<ProductDTO> result = new ArrayList<>();
            for (Long id : ids) {
                if (productMap.containsKey(id)) {
                    result.add(modelMapper.map(productMap.get(id), ProductDTO.class));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to fetch trending products", e);
            return Collections.emptyList();
        }
    }

    private String getCurrentDailyKey() {
        // Daily rolling window for trending
        return TRENDING_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }
}
