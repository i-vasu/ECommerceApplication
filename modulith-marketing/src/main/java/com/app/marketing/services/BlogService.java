package com.app.marketing.services;

import com.app.marketing.entities.Blog;

import com.app.marketing.payloads.BlogDTO;
import java.util.List;

public interface BlogService {
    BlogDTO createBlog(BlogDTO blogDTO);

    BlogDTO publishBlog(Long blogId);

    List<BlogDTO> getAllPublishedBlogs();

    BlogDTO getBlogById(Long blogId);
}
