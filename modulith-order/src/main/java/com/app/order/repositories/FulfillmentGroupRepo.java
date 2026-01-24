package com.app.order.repositories;

import com.app.order.entities.FulfillmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FulfillmentGroupRepo extends JpaRepository<FulfillmentGroup, Long> {
}
