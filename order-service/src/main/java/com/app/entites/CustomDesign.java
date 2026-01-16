package com.app.entites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_designs")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private java.util.Map<String, String> choices = new java.util.HashMap<>();

    // Optional: If user can upload a reference image or sketch
    private String referenceImageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}
