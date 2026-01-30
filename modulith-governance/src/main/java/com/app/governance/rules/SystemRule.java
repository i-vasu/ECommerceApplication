package com.app.governance.rules;

import jakarta.persistence.*;

@Entity
@Table(name = "system_rules")
public class SystemRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    @Column(unique = true, nullable = false)
    private String ruleKey; // e.g., "CHECKOUT_POLICY"

    @Column(length = 2000, nullable = false)
    private String expression; // The SpEL string

    private String description;

    private String category; // e.g., "FRAUD", "CHECKOUT", "RETURN"

    private boolean active = true;

    // Constructors
    public SystemRule() {
    }

    public SystemRule(Long ruleId, String ruleKey, String expression, String description, String category, boolean active) {
        this.ruleId = ruleId;
        this.ruleKey = ruleKey;
        this.expression = expression;
        this.description = description;
        this.category = category;
        this.active = active;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long ruleId;
        private String ruleKey;
        private String expression;
        private String description;
        private String category;
        private boolean active = true;

        public Builder ruleId(Long ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder ruleKey(String ruleKey) {
            this.ruleKey = ruleKey;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public SystemRule build() {
            return new SystemRule(ruleId, ruleKey, expression, description, category, active);
        }
    }

    // Getters and Setters
    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
