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

	@Query("SELECT a FROM Address a JOIN a.profiles p JOIN p.user u WHERE u.userId = :userId")
	org.springframework.data.domain.Page<Address> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

}
