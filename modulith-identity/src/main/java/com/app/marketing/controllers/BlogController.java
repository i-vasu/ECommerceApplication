package com.app.marketing.controllers;

import com.app.marketing.entities.Blog;
import com.app.marketing.services.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
