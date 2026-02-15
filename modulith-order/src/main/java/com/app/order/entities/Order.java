package com.app.order.entities;

import com.app.governance.states.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Aggregate Root following DDD principles.
 * Encapsulates state transitions and business invariants.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @jakarta.persistence.Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Email
    @Column(nullable = false)
    private String email;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private LocalDate orderDate;

    private Long paymentId;

    private Long userId;

    private Long shipmentId;

    // Use BigDecimal for production-grade financial precision
    private BigDecimal subTotal = BigDecimal.ZERO;
    private BigDecimal totalTax = BigDecimal.ZERO;
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.PRIVATE) // Controlled via domain methods
    private OrderStatus orderStatus;

    private LocalDateTime deliveredDate;

    private String couponCode;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String inventoryLockId;


    // Address Snapshot (Ubiquitous Language: Shipping Destination)
    // Ensures persistent records even if the user updates their profile address
    // later.
    private String shippingStreet;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String shippingCountry;
    private String shippingReceiverPhone;

    // ==================== Domain Business Methods ====================

    /**
     * Initializes the order with a shipping destination snapshot.
     */
    public void setShippingDestination(String street, String city, String state, String pincode, String country, String phone) {
        this.shippingStreet = street;
        this.shippingCity = city;
        this.shippingState = state;
        this.shippingPincode = pincode;
        this.shippingCountry = country;
        this.shippingReceiverPhone = phone;
    }

    public void markAsPaid(String pgPaymentId) {
        this.orderStatus = OrderStatus.PAYMENT_CAPTURED;
    }

    /**
     * Domain method to add an item to the order.
     * Encapsulates the relationship and invariant check.
     */
    public void addItem(Long productId, String productName, String itemCode, Double quantity, BigDecimal price, BigDecimal discount) {
        OrderItem item = new OrderItem();
        item.setOrder(this);
        item.setProductId(productId);
        item.setQuantity(quantity.intValue());
        item.setOrderedPrice(price);
        item.setDiscount(discount);
        item.setProductName(productName);
        item.setItemCode(itemCode);
        item.setStatus("NORMAL");

        this.orderItems.add(item);
        calculateTotals();
    }

    /**
     * Recalculates subtotal and total amount based on items.
     */
    public void calculateTotals() {
        this.subTotal = orderItems.stream()
                .map(item -> item.getOrderedPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = this.subTotal.add(this.totalTax).add(this.shippingCost).subtract(this.discountAmount);
    }

    // Manual setters for fields
    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }



    public void setOrderStatus(OrderStatus status) {
        this.orderStatus = status;
    }

    public void setDeliveredDate(LocalDateTime deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public void setOrderItems(List<OrderItem> items) {
        this.orderItems = items;
        if (items != null) {
            items.stream().filter(i -> i != null).forEach(i -> i.setOrder(this));
        }
    }

    public Long getOrderId() { return orderId; }
    public String getEmail() { return email; }
    public Long getUserId() { return userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getShippingReceiverPhone() { return shippingReceiverPhone; }
    
    public String getShippingStreet() { return shippingStreet; }
    public String getShippingCity() { return shippingCity; }
    public String getShippingState() { return shippingState; }
    public String getShippingPincode() { return shippingPincode; }
    public String getShippingCountry() { return shippingCountry; }

    public String getInventoryLockId() { return inventoryLockId; }
    public void setInventoryLockId(String inventoryLockId) { this.inventoryLockId = inventoryLockId; }
    
    public OrderStatus getOrderStatus() { return orderStatus; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public String getCouponCode() { return couponCode; }
    public Long getPaymentId() { return paymentId; }
    public Long getShipmentId() { return shipmentId; }
    public LocalDateTime getDeliveredDate() { return deliveredDate; }
    public LocalDate getOrderDate() { return orderDate; }
    public BigDecimal getSubTotal() { return subTotal; }
    public BigDecimal getTotalTax() { return totalTax; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
}
