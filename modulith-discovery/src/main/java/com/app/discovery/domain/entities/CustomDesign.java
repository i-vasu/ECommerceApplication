package com.app.discovery.domain.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "custom_designs")
public class CustomDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long designId;

    private Long userId; // The designer/user

    private Long baseProductId; // The product being customized (Saree A, Kurti B)

    // Using JSON or simpler Key-Value map for strict choices
    // "Fabric" -> "Kanjivaram Silk", "Color" -> "Maroon", "Border" -> "Peacock
    // Gold"
    @ElementCollection
    @CollectionTable(name = "design_choices", joinColumns = @JoinColumn(name = "design_id"))
    @MapKeyColumn(name = "choice_key")
    @Column(name = "choice_value")
    private Map<String, String> choices = new HashMap<>();

    // Optional: If user can upload a reference image or sketch
    private String referenceImageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();

    public CustomDesign() {
    }

    public CustomDesign(Long designId, Long userId, Long baseProductId, Map<String, String> choices,
            String referenceImageUrl, LocalDateTime createdAt) {
        this.designId = designId;
        this.userId = userId;
        this.baseProductId = baseProductId;
        this.choices = choices;
        this.referenceImageUrl = referenceImageUrl;
        this.createdAt = createdAt;
    }

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBaseProductId() {
        return baseProductId;
    }

    public void setBaseProductId(Long baseProductId) {
        this.baseProductId = baseProductId;
    }

    public Map<String, String> getChoices() {
        return choices;
    }

    public void setChoices(Map<String, String> choices) {
        this.choices = choices;
    }

    public String getReferenceImageUrl() {
        return referenceImageUrl;
    }

    public void setReferenceImageUrl(String referenceImageUrl) {
        this.referenceImageUrl = referenceImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
