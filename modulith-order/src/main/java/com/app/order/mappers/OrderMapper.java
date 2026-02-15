package com.app.order.mappers;

import com.app.core.payloads.PaymentDTO;
import com.app.finance.entities.Payment;
import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @org.mapstruct.Mapping(target = "shippingReceiverPhone", source = "shippingReceiverPhone")
    OrderDTO orderToOrderDTO(Order order);

    Order orderDTOToOrder(OrderDTO orderDTO);

    @org.mapstruct.Mapping(target = "product.productId", source = "productId")
    @org.mapstruct.Mapping(target = "product.productName", source = "productName")
    @org.mapstruct.Mapping(target = "product.itemCode", source = "itemCode")
    @org.mapstruct.Mapping(target = "orderedProductPrice", source = "orderedPrice")
    OrderItemDTO orderItemToOrderItemDTO(OrderItem orderItem);

    @org.mapstruct.Mapping(target = "productId", source = "product.productId")
    @org.mapstruct.Mapping(target = "productName", source = "product.productName")
    @org.mapstruct.Mapping(target = "itemCode", source = "product.itemCode")
    @org.mapstruct.Mapping(target = "orderedPrice", source = "orderedProductPrice")
    OrderItem orderItemDTOToOrderItem(OrderItemDTO orderItemDTO);

    PaymentDTO paymentToPaymentDTO(Payment payment);

    Payment paymentDTOToPayment(PaymentDTO paymentDTO);
}
