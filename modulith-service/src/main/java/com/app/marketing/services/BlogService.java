package com.app.marketing.services;

import com.app.order.entites.Blog;
import java.util.List;

public interface BlogService {
    Blog createBlog(Blog blog);

    Blog publishBlog(Long blogId);

    List<Blog> getAllPublishedBlogs();

    Blog getBlogById(Long blogId);
}
