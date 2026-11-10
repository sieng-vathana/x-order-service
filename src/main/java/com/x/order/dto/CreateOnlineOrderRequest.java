package com.x.order.dto;

import com.x.order.entity.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateOnlineOrderRequest(
        @NotNull @Positive Long businessId,
        @NotNull @Positive Long storeId,
        /** Null for a guest order; contactName and contactPhone are then required. */
        @Positive Long customerId,
        @Size(max = 160) String contactName,
        @Size(max = 32) String contactPhone,
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

    @AssertTrue(message = "A guest order needs a contact name and phone")
    public boolean hasGuestContact() {
        return customerId != null
                || (contactName != null && !contactName.isBlank() && contactPhone != null && !contactPhone.isBlank());
    }

    @AssertTrue(message = "A delivery quote and delivery address are required unless the customer picks up")
    public boolean hasFulfillmentDetails() {
        return fulfillmentType == FulfillmentType.CUSTOMER_PICKUP
                || (deliveryQuoteId != null && deliveryAddress != null);
    }
}
