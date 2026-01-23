package com.app.order.payloads;

import lombok.Data;

@Data
public class TicketDTO {
    private String userEmail;
    private String subject;
    private String message;
    private Long relatedOrderId;
}
