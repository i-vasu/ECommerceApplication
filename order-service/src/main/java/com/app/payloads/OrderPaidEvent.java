package com.app.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent implements Serializable {
    private Long orderId;
    private String email;
    private Double amount;
    private String pgPaymentId;
}
