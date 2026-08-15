package com.x.order.controller;

import com.x.order.dto.CreateHeldPosOrderRequest;
import com.x.order.dto.OrderResponse;
import com.x.order.dto.PosOrderItemRequest;
import com.x.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Test
    void createHeldUsesSharedApiResponseEnvelope() {
        OrderController controller = new OrderController(orderService);
        CreateHeldPosOrderRequest request = new CreateHeldPosOrderRequest(
                1L, 2L, 3L, null, 4L, "USD", BigDecimal.ZERO,
                "Paused from this register", "HOLD-123", List.of(
                new PosOrderItemRequest(10L, 2, null, null, null)));
        when(orderService.createHeld(request)).thenReturn(null);

        var response = controller.createHeld(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(1);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).isEqualTo("POS sale held");
    }

    @Test
    void listHeldUsesPagedSharedApiResponseEnvelope() {
        OrderController controller = new OrderController(orderService);
        when(orderService.listHeld(2L, 0, 50)).thenReturn(new PageImpl<>(List.<OrderResponse>of()));

        var response = controller.listHeld(2L, 0, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(1);
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().content()).isEmpty();
    }
}
