package com.app.support.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {

    @Id
    private String email;

    @Enumerated(EnumType.STRING)
    private CustomerTier tier = CustomerTier.SILVER;

    private Integer rewardPoints = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> measurements; // e.g., { height: 180, chest: 100, etc }

    private String preferredStylist;

    public CustomerProfile() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public CustomerTier getTier() { return tier; }
    public void setTier(CustomerTier tier) { this.tier = tier; }

    public Integer getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }

    public Map<String, Object> getMeasurements() { return measurements; }
    public void setMeasurements(Map<String, Object> measurements) { this.measurements = measurements; }

    public String getPreferredStylist() { return preferredStylist; }
    public void setPreferredStylist(String preferredStylist) { this.preferredStylist = preferredStylist; }

    public enum CustomerTier {
        SILVER, GOLD, PLATINUM
    }
}
