package com.app.marketing.controllers;

import com.app.order.entites.Blog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Blog", description = "Blog Management APIs")
@SecurityRequirement(name = "E-Commerce Application")
public interface BlogApi {

    @Operation(summary = "Create Draft Blog", description = "Creates a new blog draft (Admin only)")
    @ApiResponse(responseCode = "200", description = "Draft created")
    @PostMapping("/admin")
    ResponseEntity<Blog> createDraft(@RequestBody Blog blog);

    @Operation(summary = "Publish Blog", description = "Publishes a blog draft (Admin only)")
    @ApiResponse(responseCode = "200", description = "Blog published")
    @PostMapping("/admin/{blogId}/publish")
    ResponseEntity<Blog> publish(@PathVariable Long blogId);

    @Operation(summary = "Get Published Blogs", description = "Retrieves all published blogs")
    @ApiResponse(responseCode = "200", description = "Blogs retrieved")
    @GetMapping("/public")
    ResponseEntity<List<Blog>> getBlogs();

    @Operation(summary = "Get Blog Details", description = "Retrieves a specific blog by ID")
    @ApiResponse(responseCode = "200", description = "Blog found")
    @GetMapping("/public/{blogId}")
    ResponseEntity<Blog> getBlog(@PathVariable Long blogId);
}
