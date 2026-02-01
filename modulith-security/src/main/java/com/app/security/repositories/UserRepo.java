package com.app.security.repositories;

import com.app.security.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

	@Query("SELECT u FROM User u JOIN FETCH u.profile p JOIN FETCH p.addresses a WHERE a.addressId = ?1")
	List<User> findByAddress(Long addressId);

	Optional<User> findByEmail(String email);

	Optional<User> findByVerificationCode(String verificationCode);

	Optional<User> findByResetToken(String resetToken);

    @Query("SELECT u FROM User u JOIN u.profile p WHERE p.mobileNumber = ?1")
	Optional<User> findByMobileNumber(String mobileNumber);
}
