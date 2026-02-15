package com.app.order.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item_tax_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemTaxDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    private String taxName; // e.g. "CGST", "SGST", "IGST"
    private java.math.BigDecimal taxRate; // e.g. 9.0
    private java.math.BigDecimal taxAmount; // calculated amount

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }

    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }

    public java.math.BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(java.math.BigDecimal taxRate) { this.taxRate = taxRate; }

    public java.math.BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(java.math.BigDecimal taxAmount) { this.taxAmount = taxAmount; }
}
