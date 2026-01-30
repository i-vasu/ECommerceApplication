package com.app.governance.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Operational Audit Entity.
 * Captures all state transitions and rule engine evaluations for the Admin
 * Dashboard.
 */
@Entity
@Table(name = "operational_audit_logs", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_category", columnList = "category")
})
public class OperationalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // STATE_TRANSITION or RULE_EVALUATION

    private String category; // Order, Payment, Inventory, Promotion, etc.

    private String entityId;

    private String detail; // "PAID -> SHIPPED" or "Rule 'Fraud_Check' matched"

    private boolean success;

    private String result; // Optional: "TRUE", "90.0", etc.

    @CreationTimestamp
    private LocalDateTime timestamp;

    // Constructors
    public OperationalAudit() {
    }

    public OperationalAudit(String type, String category, String entityId, String detail, boolean success, String result) {
        this.type = type;
        this.category = category;
        this.entityId = entityId;
        this.detail = detail;
        this.success = success;
        this.result = result;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String category;
        private String entityId;
        private String detail;
        private boolean success;
        private String result;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder result(String result) {
            this.result = result;
            return this;
        }

        public OperationalAudit build() {
            return new OperationalAudit(type, category, entityId, detail, success, result);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationalAudit that = (OperationalAudit) o;
        return success == that.success &&
                Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(category, that.category) &&
                Objects.equals(entityId, that.entityId) &&
                Objects.equals(detail, that.detail) &&
                Objects.equals(result, that.result) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, category, entityId, detail, success, result, timestamp);
    }

    @Override
    public String toString() {
        return "OperationalAudit{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", entityId='" + entityId + '\'' +
                ", detail='" + detail + '\'' +
                ", success=" + success +
                ", result='" + result + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
