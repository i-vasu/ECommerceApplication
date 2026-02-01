package com.app.catalog.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "size_charts")
@Data
public class SizeChart {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name; // e.g., "Men's Shirts"

    @ElementCollection
    @CollectionTable(name = "size_chart_data", joinColumns = @JoinColumn(name = "size_chart_id"))
    @MapKeyColumn(name = "measurement_name")
    @Column(name = "measurement_value")
    private Map<String, String> data = new HashMap<>(); // e.g., {"Chest": "40-42", "Length": "28"}

    @OneToOne(mappedBy = "sizeChart")
    private Category category;
}
