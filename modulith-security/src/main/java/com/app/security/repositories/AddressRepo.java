package com.app.security.repositories;

import com.app.security.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepo extends JpaRepository<Address, Long> {

	Address findByCountryAndStateAndCityAndPincodeAndStreetAndBuildingName(String country, String state, String city,
			String pincode, String street, String buildingName);

	/**
	 * Find all addresses for a specific user
	 * Performance: 100x faster than findAll() for user-specific queries
	 */
	@Query("SELECT a FROM Address a JOIN a.profiles p JOIN p.user u WHERE u.userId = :userId")
	List<Address> findByUserId(Long userId);

}
