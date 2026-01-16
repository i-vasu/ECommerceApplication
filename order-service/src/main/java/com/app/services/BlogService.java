package com.app.services;

import com.app.entites.Blog;
import java.util.List;

public interface BlogService {
    Blog createBlog(Blog blog);

    Blog publishBlog(Long blogId);

    List<Blog> getAllPublishedBlogs();

    Blog getBlogById(Long blogId);
}
