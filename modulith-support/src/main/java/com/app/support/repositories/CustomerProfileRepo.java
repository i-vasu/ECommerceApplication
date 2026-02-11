package com.app.support.repositories;

import com.app.support.entities.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepo extends JpaRepository<CustomerProfile, String> {
}
