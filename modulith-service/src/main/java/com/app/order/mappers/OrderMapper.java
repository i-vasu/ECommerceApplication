package com.app.order.mappers;

import com.app.order.entites.Order;
import com.app.order.entites.OrderItem;
import com.app.order.entites.Payment;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderItemDTO;
import com.app.order.payloads.PaymentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    OrderDTO orderToOrderDTO(Order order);

    Order orderDTOToOrder(OrderDTO orderDTO);

    OrderItemDTO orderItemToOrderItemDTO(OrderItem orderItem);

    OrderItem orderItemDTOToOrderItem(OrderItemDTO orderItemDTO);

    PaymentDTO paymentToPaymentDTO(Payment payment);

    Payment paymentDTOToPayment(PaymentDTO paymentDTO);
}
