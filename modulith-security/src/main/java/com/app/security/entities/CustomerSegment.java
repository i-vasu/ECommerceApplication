package com.app.security.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Customer Segment Entity.
 * Used for targeted pricing, promotions, and content.
 * Mirrors Broadleaf's 'CustomerSegment' system.
 */
@Entity
@Table(name = "customer_segments")
public class CustomerSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Premium VIP", "New Users", "Fashion Enthusiasts"
    private String description;

    @Column(length = 1000)
    private String ruleExpression; // SpEL, e.g., "#user.totalSpent > 10000"

    @ManyToMany(mappedBy = "segments")
    private Set<UserLoyalty> loyalties = new HashSet<>();

    public CustomerSegment() {}
    public CustomerSegment(Long id, String name, String description, String ruleExpression, Set<UserLoyalty> loyalties) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ruleExpression = ruleExpression;
        this.loyalties = loyalties;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleExpression() { return ruleExpression; }
    public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }
    public Set<UserLoyalty> getLoyalties() { return loyalties; }
    public void setLoyalties(Set<UserLoyalty> loyalties) { this.loyalties = loyalties; }
}
