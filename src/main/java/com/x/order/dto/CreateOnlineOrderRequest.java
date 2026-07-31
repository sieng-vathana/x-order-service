package com.x.order.dto;

import com.x.order.entity.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateOnlineOrderRequest(
        @NotNull @Positive Long businessId,
        @NotNull @Positive Long storeId,
        @NotNull @Positive Long customerId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
        @NotNull FulfillmentType fulfillmentType,
        @NotNull OnlinePaymentType paymentType,
        @Positive Long deliveryQuoteId,
        @Valid DeliveryAddressRequest deliveryAddress,
        @Pattern(regexp = "[A-Za-z0-9_-]{2,64}") String discountCode,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
        @Size(max = 500) String note,
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotEmpty List<@Valid OnlineOrderItemRequest> items) {

    @AssertTrue(message = "A delivery quote and delivery address are required unless the customer picks up")
    public boolean hasFulfillmentDetails() {
        return fulfillmentType == FulfillmentType.CUSTOMER_PICKUP
                || (deliveryQuoteId != null && deliveryAddress != null);
    }
}
