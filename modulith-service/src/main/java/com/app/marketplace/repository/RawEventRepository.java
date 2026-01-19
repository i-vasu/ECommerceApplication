package com.app.marketplace.repository;

import com.app.marketplace.model.RawEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawEventRepository extends JpaRepository<RawEvent, Long> {
}
