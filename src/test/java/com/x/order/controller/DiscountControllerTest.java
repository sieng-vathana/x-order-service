package com.x.order.controller;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.DiscountRequest;
import com.x.order.dto.DiscountResponse;
import com.x.order.entity.*;
import com.x.order.service.DiscountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DiscountControllerTest {

    @Mock
    private DiscountService discountService;

    @Test
    void createUsesSharedApiResponseEnvelope() {
        DiscountController controller = new DiscountController(discountService);
        DiscountRequest request = new DiscountRequest(
                1L,
                "Welcome",
                "WELCOME10",
                DiscountType.PERCENTAGE,
                DiscountScope.ORDER,
                DiscountChannel.BOTH,
                new BigDecimal("10.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                DiscountStatus.ACTIVE,
                Set.of(),
                Set.of(),
                Set.of());

        ResponseEntity<ApiResponse<DiscountResponse>> response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(1);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).isEqualTo("Discount created");
    }
}
