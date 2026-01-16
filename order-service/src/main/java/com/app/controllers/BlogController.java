package com.app.controllers;

import com.app.entites.Blog;
import com.app.services.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @PostMapping("/admin")
    public ResponseEntity<Blog> createDraft(@RequestBody Blog blog) {
        return ResponseEntity.ok(blogService.createBlog(blog));
    }

    @PostMapping("/admin/{blogId}/publish")
    public ResponseEntity<Blog> publish(@PathVariable Long blogId) {
        return ResponseEntity.ok(blogService.publishBlog(blogId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<Blog>> getBlogs() {
        return ResponseEntity.ok(blogService.getAllPublishedBlogs());
    }

    @GetMapping("/public/{blogId}")
    public ResponseEntity<Blog> getBlog(@PathVariable Long blogId) {
        return ResponseEntity.ok(blogService.getBlogById(blogId));
    }
}
