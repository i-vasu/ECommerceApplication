package com.app.marketing.repositories;

import com.app.marketing.entities.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepo extends JpaRepository<Blog, Long> {
    List<Blog> findByStatusOrderByCreatedAtDesc(String status);
}
