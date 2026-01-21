package com.app.marketplace.service;

import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDispatchServiceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderDispatchService orderDispatchService;

    @Test
    void testDispatch_Success() {
        OrderDTO orderDTO = OrderDTO.builder().orderId(123L).build();

        orderDispatchService.dispatch(orderDTO);

        verify(orderService, times(1)).placeMarketplaceOrder(orderDTO);
    }

    @Test
    void testDispatch_Failure() {
        OrderDTO orderDTO = OrderDTO.builder().orderId(456L).build();
        doThrow(new RuntimeException("Database down")).when(orderService).placeMarketplaceOrder(orderDTO);

        assertThrows(RuntimeException.class, () -> orderDispatchService.dispatch(orderDTO));
        verify(orderService, times(1)).placeMarketplaceOrder(orderDTO);
    }
}
