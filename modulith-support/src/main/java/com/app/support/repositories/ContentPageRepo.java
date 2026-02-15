package com.app.support.repositories;

import com.app.support.entities.ContentPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentPageRepo extends JpaRepository<ContentPage, Long> {
    Optional<ContentPage> findBySlug(String slug);
}
