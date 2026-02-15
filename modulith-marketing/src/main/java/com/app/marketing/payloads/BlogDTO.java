package com.app.marketing.payloads;

import java.time.LocalDateTime;

public class BlogDTO {

    private Long blogId;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private String status;

    public BlogDTO() {
    }

    public BlogDTO(Long blogId, String title, String content, String author, LocalDateTime createdAt, String status) {
        this.blogId = blogId;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
