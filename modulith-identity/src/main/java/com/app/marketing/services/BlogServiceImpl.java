package com.app.marketing.services;

import com.app.marketing.repositories.BlogRepo;
import com.app.marketing.entities.Blog;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class BlogServiceImpl implements BlogService {

    private static final String BLOG_EVENTS_STREAM = "blog-events";

    private final BlogRepo blogRepo;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Blog createBlog(Blog blog) {
        if (blog.getCreatedAt() == null)
            blog.setCreatedAt(LocalDateTime.now());
        if (blog.getStatus() == null)
            blog.setStatus("DRAFT");
        return blogRepo.save(blog);
    }

    @Override
    public Blog publishBlog(Long blogId) {
        var blog = blogRepo.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        blog.setStatus("PUBLISHED");
        var saved = blogRepo.save(blog);

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
            var content = blog.getContent() != null ? blog.getContent() : "";
            eventData.put("content", content);

            ObjectRecord<String, Map<String, String>> record = StreamRecords
                    .newRecord()
                    .ofObject(eventData)
                    .withStreamKey(BLOG_EVENTS_STREAM);

            redisTemplate.opsForStream().add(record);

            log.info("Published Blog Broadcast Event to Redis Stream: {}", blog.getTitle());

        } catch (Exception e) {
            log.error("Failed to publish blog event: {}", e.getMessage());
        }
    }

    @Override
    public List<Blog> getAllPublishedBlogs() {
        return blogRepo.findByStatusOrderByCreatedAtDesc("PUBLISHED");
    }

    @Override
    public Blog getBlogById(Long blogId) {
        return blogRepo.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
    }
}
