package com.app.entites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blogId;

    private String title;

    @Column(length = 5000)
    private String content;

    private String author; // e.g. "Admin"

    private LocalDateTime createdAt = LocalDateTime.now();

    // e.g. "PUBLISHED", "DRAFT"
    private String status = "DRAFT";
}
