package com.app.finance.pricing.contracts;

import java.util.ArrayList;
import java.util.List;

/**
 * Context required by Modules to calculate Totals.
 */

public class OrderTotalInput {
    // Current Cart/Order ID
    private Long id;

    // User Email (for personalized discounts)
    private String email;

    // User ID
    private Long userId;

    // Address (for Tax/Shipping)
    private String shippingCountry;
    private String shippingState;
    private String shippingZip;

    // Currency
    private String currencyCode;

    // Coupon Code
    private String couponCode;

    // Items for calculation
    private List<ItemInput> items = new ArrayList<>();

    public OrderTotalInput() {
    }

    public OrderTotalInput(Long id, String email, Long userId, String shippingCountry, String shippingState, String shippingZip,
            String currencyCode, String couponCode, List<ItemInput> items) {
        this.id = id;
        this.email = email;
        this.userId = userId;
        this.shippingCountry = shippingCountry;
        this.shippingState = shippingState;
        this.shippingZip = shippingZip;
        this.currencyCode = currencyCode;
        this.couponCode = couponCode;
        this.items = items;
    }

    // Manual Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public String getShippingState() {
        return shippingState;
    }

    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }

    public String getShippingZip() {
        return shippingZip;
    }

    public void setShippingZip(String shippingZip) {
        this.shippingZip = shippingZip;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public List<ItemInput> getItems() {
        return items;
    }

    public void setItems(List<ItemInput> items) {
        this.items = items;
    }

    // Manual Builder
    public static class OrderTotalInputBuilder {
        private Long id;
        private String email;
        private Long userId;
        private String shippingCountry;
        private String shippingState;
        private String shippingZip;
        private String currencyCode;
        private String couponCode;
        private List<ItemInput> items = new ArrayList<>();

        public OrderTotalInputBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderTotalInputBuilder email(String email) {
            this.email = email;
            return this;
        }

        public OrderTotalInputBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public OrderTotalInputBuilder shippingCountry(String shippingCountry) {
            this.shippingCountry = shippingCountry;
            return this;
        }

        public OrderTotalInputBuilder shippingState(String shippingState) {
            this.shippingState = shippingState;
            return this;
        }

        public OrderTotalInputBuilder shippingZip(String shippingZip) {
            this.shippingZip = shippingZip;
            return this;
        }

        public OrderTotalInputBuilder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public OrderTotalInputBuilder couponCode(String couponCode) {
            this.couponCode = couponCode;
            return this;
        }

        public OrderTotalInputBuilder items(List<ItemInput> items) {
            this.items = items;
            return this;
        }

        public OrderTotalInput build() {
            return new OrderTotalInput(id, email, userId, shippingCountry, shippingState, shippingZip, currencyCode, couponCode,
                    items);
        }
    }

    public static OrderTotalInputBuilder builder() {
        return new OrderTotalInputBuilder();
    }

    public static class ItemInput {
        private Long productId;
        private String itemCode;
        private java.math.BigDecimal price;
        private Integer quantity;

        public ItemInput() {
        }

        public ItemInput(Long productId, String itemCode, java.math.BigDecimal price, Integer quantity) {
            this.productId = productId;
            this.itemCode = itemCode;
            this.price = price;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public java.math.BigDecimal getPrice() {
            return price;
        }

        public void setPrice(java.math.BigDecimal price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
