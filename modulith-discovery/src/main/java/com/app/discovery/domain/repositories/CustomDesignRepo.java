package com.app.discovery.domain.repositories;

import com.app.discovery.domain.entities.CustomDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomDesignRepo extends JpaRepository<CustomDesign, Long> {
    List<CustomDesign> findByUserId(Long userId);
}
