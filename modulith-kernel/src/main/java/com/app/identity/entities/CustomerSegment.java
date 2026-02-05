package com.app.identity.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Customer Segment Entity.
 * Used for targeted pricing, promotions, and content.
 * Mirrors Broadleaf's 'CustomerSegment' system.
 */
@Entity
@Table(name = "customer_segments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Premium VIP", "New Users", "Fashion Enthusiasts"
    private String description;

    @Column(length = 1000)
    private String ruleExpression; // SpEL, e.g., "#user.totalSpent > 10000"
}
