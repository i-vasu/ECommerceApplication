package com.app.marketing.repositories;

import com.app.marketing.entities.UserTrait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTraitRepo extends JpaRepository<UserTrait, String> {
}
