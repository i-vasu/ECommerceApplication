package com.app.identity.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.identity.entities.Role;

@Repository
public interface RoleRepo extends JpaRepository<Role, Long> {

}
