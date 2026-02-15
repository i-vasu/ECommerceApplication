package com.app.security.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_loyalty")
@Getter
@Setter
@NoArgsConstructor
public class UserLoyalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loyaltyId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    @JsonIgnore
    private User user;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    @Column(name = "customer_group")
    private String customerGroup = "RETAIL"; // Default to RETAIL

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "user_segment_map", joinColumns = @JoinColumn(name = "loyalty_id"), inverseJoinColumns = @JoinColumn(name = "segment_id"))
    private Set<CustomerSegment> segments = new HashSet<>();

    // Manual Getters and Setters
    public Long getLoyaltyId() {
        return loyaltyId;
    }

    public void setLoyaltyId(Long loyaltyId) {
        this.loyaltyId = loyaltyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(Integer rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public String getCustomerGroup() {
        return customerGroup;
    }

    public void setCustomerGroup(String customerGroup) {
        this.customerGroup = customerGroup;
    }

    public Set<CustomerSegment> getSegments() {
        return segments;
    }

    public void setSegments(Set<CustomerSegment> segments) {
        this.segments = segments;
    }
}
