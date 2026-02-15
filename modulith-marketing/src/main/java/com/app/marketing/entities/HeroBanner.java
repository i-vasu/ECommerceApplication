package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "hero_banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeroBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl; // URL to the image/video
    
    private String targetUrl; // Link when clicked
    
    private Integer displayOrder; // 1, 2, 3...
    
    private boolean isActive = true;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
