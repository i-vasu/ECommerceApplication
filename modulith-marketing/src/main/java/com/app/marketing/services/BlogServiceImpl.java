package com.app.marketing.services;

import com.app.marketing.entities.Blog;
import com.app.marketing.repositories.BlogRepo;
import com.app.marketing.payloads.BlogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RequiredArgsConstructor
@Service
public class BlogServiceImpl implements BlogService {

    private static final String BLOG_EVENTS_STREAM = "blog-events";

    private final BlogRepo blogRepo;
    private final StringRedisTemplate redisTemplate;
    private final com.app.marketing.mappers.BlogMapper blogMapper;

    @Override
    public BlogDTO createBlog(BlogDTO blogDTO) {
        Blog blog = blogMapper.blogDTOToBlog(blogDTO);
        if (blog.getCreatedAt() == null)
            blog.setCreatedAt(LocalDateTime.now());
        if (blog.getStatus() == null)
            blog.setStatus("DRAFT");
        
        Blog savedBlog = blogRepo.save(blog);
        return blogMapper.blogToBlogDTO(savedBlog);
    }

    @Override
    public BlogDTO publishBlog(Long blogId) {
        var blog = blogRepo.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        blog.setStatus("PUBLISHED");
        var saved = blogRepo.save(blog);

        // Trigger Async Mailing
        broadcastBlog(saved);

        return blogMapper.blogToBlogDTO(saved);
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
    public List<BlogDTO> getAllPublishedBlogs() {
        List<Blog> blogs = blogRepo.findByStatusOrderByCreatedAtDesc("PUBLISHED");
        return blogs.stream().map(blogMapper::blogToBlogDTO).toList();
    }

    @Override
    public BlogDTO getBlogById(Long blogId) {
        Blog blog = blogRepo.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        return blogMapper.blogToBlogDTO(blog);
    }
}
