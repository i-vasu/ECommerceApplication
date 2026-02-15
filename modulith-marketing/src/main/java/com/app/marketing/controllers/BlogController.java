package com.app.marketing.controllers;

import com.app.marketing.entities.Blog;
import com.app.marketing.payloads.BlogDTO;
import com.app.marketing.services.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blogs")
public class BlogController implements BlogApi {

    @Autowired
    private BlogService blogService;

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<BlogDTO> createDraft(@RequestBody BlogDTO blogDTO) {
        return ResponseEntity.ok(blogService.createBlog(blogDTO));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<BlogDTO> publish(@PathVariable Long blogId) {
        return ResponseEntity.ok(blogService.publishBlog(blogId));
    }

    @Override
    public ResponseEntity<List<BlogDTO>> getBlogs() {
        return ResponseEntity.ok(blogService.getAllPublishedBlogs());
    }

    @Override
    public ResponseEntity<BlogDTO> getBlog(@PathVariable Long blogId) {
        return ResponseEntity.ok(blogService.getBlogById(blogId));
    }
}
