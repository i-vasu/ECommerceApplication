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

}
