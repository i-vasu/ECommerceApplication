package com.app.marketing.controllers;

import com.app.order.entites.Blog;
import com.app.marketing.services.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("/api/v1/blogs")
public class BlogController implements BlogApi {

    @Autowired
    private BlogService blogService;

    @Override
    public ResponseEntity<Blog> createDraft(@RequestBody Blog blog) {
        return ResponseEntity.ok(blogService.createBlog(blog));
    }

    @Override
    public ResponseEntity<Blog> publish(@PathVariable Long blogId) {
        return ResponseEntity.ok(blogService.publishBlog(blogId));
    }

    @Override
    public ResponseEntity<List<Blog>> getBlogs() {
        return ResponseEntity.ok(blogService.getAllPublishedBlogs());
    }

    @Override
    public ResponseEntity<Blog> getBlog(@PathVariable Long blogId) {
        return ResponseEntity.ok(blogService.getBlogById(blogId));
    }
}
