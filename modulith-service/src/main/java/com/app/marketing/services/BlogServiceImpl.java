package com.app.marketing.services;

import com.app.marketing.repositories.BlogRepo;
import com.app.order.entites.Blog;
import com.app.identity.repositories.UserRepo;
import com.app.identity.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogRepo blogRepo;

    @Autowired
    private MarketingService marketingService;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public Blog createBlog(Blog blog) {
        if (blog.getCreatedAt() == null)
            blog.setCreatedAt(java.time.LocalDateTime.now());
        if (blog.getStatus() == null)
            blog.setStatus("DRAFT");
        return blogRepo.save(blog);
    }

    @Override
    public Blog publishBlog(Long blogId) {
        Blog blog = blogRepo.findById(blogId).orElseThrow(() -> new RuntimeException("Blog not found"));
        blog.setStatus("PUBLISHED");
        Blog saved = blogRepo.save(blog);

        // Trigger Async Mailing
        broadcastBlog(saved);

        return saved;
    }

    @Async
    public void broadcastBlog(Blog blog) {
        try {
            Map<String, String> eventData = new HashMap<>();
            eventData.put("type", "BLOG_PUBLISHED");
            eventData.put("title", blog.getTitle());
            String content = blog.getContent() != null ? blog.getContent() : "";
            eventData.put("content", content);

            // Generate JSON if needed or just send map

            org.springframework.data.redis.connection.stream.ObjectRecord<String, Map<String, String>> record = org.springframework.data.redis.connection.stream.StreamRecords
                    .newRecord()
                    .ofObject(eventData)
                    .withStreamKey(com.app.order.config.OrderRedisConfig.BLOG_EVENTS_STREAM);

            redisTemplate.opsForStream().add(record);

            System.out.println("Published Blog Broadcast Event to Redis Stream: " + blog.getTitle());

        } catch (Exception e) {
            System.err.println("Failed to publish blog event: " + e.getMessage());
        }
    }

    @Override
    public List<Blog> getAllPublishedBlogs() {
        return blogRepo.findByStatusOrderByCreatedAtDesc("PUBLISHED");
    }

    @Override
    public Blog getBlogById(Long blogId) {
        return blogRepo.findById(blogId).orElseThrow(() -> new RuntimeException("Blog not found"));
    }
}
