package com.app.security.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses")
public class Address {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long addressId;

	@NotBlank
	@Size(min = 5, message = "Street name must contain atleast 5 characters")
	private String street;

	@NotBlank
	@Size(min = 5, message = "Building name must contain atleast 5 characters")
	private String buildingName;

	@NotBlank
	@Size(min = 4, message = "City name must contain atleast 4 characters")
	private String city;

	@NotBlank
	@Size(min = 2, message = "State name must contain atleast 2 characters")
	private String state;

	@NotBlank
	@Size(min = 2, message = "Country name must contain atleast 2 characters")
	private String country;

	@NotBlank
	@Size(min = 6, message = "Pincode must contain atleast 6 characters")
	private String pincode;



	@Column(name = "label")
	private String label = "HOME"; // HOME, WORK, OTHER

	@Column(name = "receiver_phone_number")
	private String receiverPhoneNumber;

	@ManyToMany(mappedBy = "addresses")
	private List<UserProfile> profiles = new ArrayList<>();

	public Address(String country, String state, String city, String pincode, String street, String buildingName) {
		this.country = country;
		this.state = state;
		this.city = city;
		this.pincode = pincode;
		this.street = street;
		this.buildingName = buildingName;
	}

	public Address() {
	}

	public Address(Long addressId, String street, String buildingName, String city, String state, String country,
			String pincode, List<UserProfile> profiles) {
		this.addressId = addressId;
		this.street = street;
		this.buildingName = buildingName;
		this.city = city;
		this.state = state;
		this.country = country;
		this.pincode = pincode;
		this.profiles = profiles;
	}

	public Long getAddressId() {
		return addressId;
	}

	public void setAddressId(Long addressId) {
		this.addressId = addressId;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getBuildingName() {
		return buildingName;
	}

	public void setBuildingName(String buildingName) {
		this.buildingName = buildingName;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPincode() {
		return pincode;
	}

	public void setPincode(String pincode) {
		this.pincode = pincode;
	}

	public List<UserProfile> getProfiles() {
		return profiles;
	}
 
	public void setProfiles(List<UserProfile> profiles) {
		this.profiles = profiles;
	}



	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getReceiverPhoneNumber() {
		return receiverPhoneNumber;
	}

	public void setReceiverPhoneNumber(String receiverPhoneNumber) {
		this.receiverPhoneNumber = receiverPhoneNumber;
	}
}
