package com.app.support.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "return_items")
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;

    @Column(name = "order_item_id")
    private Long orderItemId;

    private Integer quantity;

    private String itemCode;

    private Double unitRefundAmount; // Pro-rata amount after discounts

    public ReturnItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReturnRequest getReturnRequest() { return returnRequest; }
    public void setReturnRequest(ReturnRequest returnRequest) { this.returnRequest = returnRequest; }

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitRefundAmount() { return unitRefundAmount; }
    public void setUnitRefundAmount(Double unitRefundAmount) { this.unitRefundAmount = unitRefundAmount; }
}
