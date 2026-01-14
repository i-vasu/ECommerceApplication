package com.app.entites;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "shipments")
@AllArgsConstructor
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentId;

    @OneToOne(mappedBy = "shipment", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Order order;

    private String carrier;
    private String awbNumber;
    private String status;
    private String trackUrl;

}
