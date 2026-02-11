package com.app.tests.fixtures;

import com.app.finance.entities.Payment;
import com.app.governance.states.OrderStatus;
import com.app.order.entities.Order;
import com.app.security.entities.User;
import com.app.security.entities.UserProfile;

import java.util.ArrayList;

/**
 * Test Data Factory for creating consistent test fixtures
 * Follows the Builder pattern for flexibility
 */
public class TestDataFactory {

    public static class OrderBuilder {
        private Order order = new Order();

        public OrderBuilder withId(Long id) {
            order.setOrderId(id);
            return this;
        }

        public OrderBuilder withStatus(OrderStatus status) {
            order.setOrderStatus(status);
            return this;
        }

        public OrderBuilder withAmount(Double amount) {
            order.setTotalAmount(java.math.BigDecimal.valueOf(amount));
            return this;
        }

        public OrderBuilder withUser(User user) {
            order.setEmail(user.getEmail());
            order.setUserId(user.getUserId());
            return this;
        }

        public OrderBuilder withPayment(Payment payment) {
            order.setPaymentId(payment.getPaymentId());
            return this;
        }

        public Order build() {
            if (order.getOrderStatus() == null)
                order.setOrderStatus(OrderStatus.PENDING);
            if (order.getTotalAmount() == null)
                order.setTotalAmount(java.math.BigDecimal.valueOf(100.0));
            if (order.getOrderItems() == null)
                order.setOrderItems(new ArrayList<>());
            return order;
        }
    }

    public static class PaymentBuilder {
        private Payment payment = new Payment();

        public PaymentBuilder withId(Long id) {
            payment.setPaymentId(id);
            return this;
        }

        public PaymentBuilder withPgOrderId(String pgOrderId) {
            payment.setPgOrderId(pgOrderId);
            return this;
        }

        public PaymentBuilder withStatus(String status) {
            payment.setPgStatus(status);
            return this;
        }

        public PaymentBuilder withMethod(String method) {
            payment.setPaymentMethod(method);
            return this;
        }

        public Payment build() {
            if (payment.getPgStatus() == null)
                payment.setPgStatus("created");
            if (payment.getPaymentMethod() == null)
                payment.setPaymentMethod("RAZORPAY");
            return payment;
        }
    }

    public static class UserBuilder {
        private User user = new User();

        public UserBuilder withEmail(String email) {
            user.setEmail(email);
            return this;
        }

        public UserBuilder withPassword(String password) {
            user.setPassword(password);
            return this;
        }

        public UserBuilder withName(String name) {
            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = new UserProfile();
                profile.setUser(user);
                user.setProfile(profile);
            }
            profile.setFirstName(name.split(" ")[0]);
            if (name.split(" ").length > 1) {
                profile.setLastName(name.split(" ")[1]);
            }
            return this;
        }

        public User build() {
            if (user.getEmail() == null)
                user.setEmail("test@example.com");
            if (user.getPassword() == null)
                user.setPassword("password123");
            return user;
        }
    }

    // Factory methods
    public static OrderBuilder order() {
        return new OrderBuilder();
    }

    public static PaymentBuilder payment() {
        return new PaymentBuilder();
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }

    // Common pre-built scenarios
    public static Order createPaidOrder() {
        Payment payment = payment()
                .withStatus("captured")
                .withPgOrderId("order_test_123")
                .build();

        return order()
                .withStatus(OrderStatus.PAYMENT_CAPTURED)
                .withAmount(500.0)
                .withPayment(payment)
                .build();
    }

    public static Order createPendingOrder() {
        return order()
                .withStatus(OrderStatus.PENDING)
                .withAmount(200.0)
                .build();
    }
}
