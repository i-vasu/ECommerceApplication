package com.app.identity.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.app.identity.entities.Role;

@Repository
public interface RoleRepo extends JpaRepository<Role, Long> {

	Optional<Role> findByRoleName(String roleName);
}
