package com.app.finance.pricing.contracts;

import java.math.BigDecimal;

public class OrderTotal {
    private String code; // subtotal, tax, shipping, total
    private String title; // "Sub Total", "GST (18%)"
    private BigDecimal value; // 100.00
    private int sortOrder; // 10, 20, 30
    private boolean isPostProcess; // If true, calculated after others

    public OrderTotal() {
    }

    public OrderTotal(String code, String title, BigDecimal value, int sortOrder, boolean isPostProcess) {
        this.code = code;
        this.title = title;
        this.value = value;
        this.sortOrder = sortOrder;
        this.isPostProcess = isPostProcess;
    }

    // Manual Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isPostProcess() {
        return isPostProcess;
    }

    public void setPostProcess(boolean postProcess) {
        isPostProcess = postProcess;
    }

    // Manual Builder
    public static class OrderTotalBuilder {
        private String code;
        private String title;
        private BigDecimal value;
        private int sortOrder;
        private boolean isPostProcess;

        public OrderTotalBuilder code(String code) {
            this.code = code;
            return this;
        }

        public OrderTotalBuilder title(String title) {
            this.title = title;
            return this;
        }

        public OrderTotalBuilder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public OrderTotalBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public OrderTotalBuilder isPostProcess(boolean isPostProcess) {
            this.isPostProcess = isPostProcess;
            return this;
        }

        public OrderTotal build() {
            return new OrderTotal(code, title, value, sortOrder, isPostProcess);
        }
    }

    public static OrderTotalBuilder builder() {
        return new OrderTotalBuilder();
    }
}
