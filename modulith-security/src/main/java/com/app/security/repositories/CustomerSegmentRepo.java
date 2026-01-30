package com.app.security.repositories;

import com.app.security.entities.CustomerSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerSegmentRepo extends JpaRepository<CustomerSegment, Long> {
    CustomerSegment findByName(String name);
}
