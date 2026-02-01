package com.app.catalog.services;

import com.app.catalog.mappers.ProductMapper;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// @Log4j2
@Service
@RequiredArgsConstructor
public class PersonalizationServiceImpl implements PersonalizationService {

    private static final int MAX_HISTORY_SIZE = 10;

    private final StringRedisTemplate redisTemplate;
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    @Override
    public void trackProductView(Long userId, Long productId) {
        if (userId == null || productId == null)
            return;

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        var key = String.format("user:%s:recently_viewed:%d", tenantId, userId);
        // Remove existing to push to front
        redisTemplate.opsForList().remove(key, 1, String.valueOf(productId));
        redisTemplate.opsForList().leftPush(key, String.valueOf(productId));
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
    }

    @Override
    public List<ProductDTO> getRecentlyViewed(Long userId, int limit) {
        if (userId == null)
            return Collections.emptyList();

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        var key = String.format("user:%s:recently_viewed:%d", tenantId, userId);
        var productIds = redisTemplate.opsForList().range(key, 0, limit - 1);

        if (productIds == null || productIds.isEmpty())
            return Collections.emptyList();

        return productIds.stream()
                .map(id -> productRepo.findById(Long.parseLong(id)).orElse(null))
                .filter(Objects::nonNull)
                .map(productMapper::productToProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getNewlyDropped(int limit) {
        var pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return productRepo.findAll(pageable).getContent().stream()
                .map(productMapper::productToProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getTrending(int limit) {
        // Mock implementation for now
        return getNewlyDropped(limit);
    }

    @Override
    public List<ProductDTO> getBestSellers(int limit) {
        // Mock implementation for now
        return getNewlyDropped(limit);
    }

    @Override
    public void trackSearch(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.isBlank())
            return;

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        var key = String.format("user:%s:search_history:%d", tenantId, userId);
        redisTemplate.opsForList().remove(key, 1, keyword);
        redisTemplate.opsForList().leftPush(key, keyword);
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
    }

    @Override
    public List<String> getSearchHistory(Long userId, int limit) {
        if (userId == null)
            return Collections.emptyList();

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        var key = String.format("user:%s:search_history:%d", tenantId, userId);
        return redisTemplate.opsForList().range(key, 0, limit - 1);
    }
}
