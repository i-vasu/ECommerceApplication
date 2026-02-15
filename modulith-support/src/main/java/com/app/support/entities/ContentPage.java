package com.app.support.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug; // e.g., "about-us", "privacy-policy"

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content; // HTML content
    
    private boolean isPublished = true;
    
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
}
