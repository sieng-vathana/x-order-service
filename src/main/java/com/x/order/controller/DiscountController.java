package com.x.order.controller;

import com.sharedlib.response.ApiResponse;
import com.sharedlib.response.PageResponse;
import com.x.order.dto.DiscountRequest;
import com.x.order.dto.DiscountResponse;
import com.x.order.service.DiscountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Validated
public class DiscountController {
    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> create(
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Discount created", discountService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> get(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long businessId) {
        return ResponseEntity.ok(ApiResponse.success(200, discountService.get(id, businessId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DiscountResponse>>> list(
            @RequestParam @Positive Long businessId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<DiscountResponse> result = discountService.list(businessId, page, size);
        return ResponseEntity.ok(ApiResponse.success(200, new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.hasNext())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> update(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long businessId,
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                200, "Discount updated", discountService.update(id, businessId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> deactivate(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long businessId) {
        return ResponseEntity.ok(ApiResponse.success(
                200, "Discount deactivated", discountService.deactivate(id, businessId)));
    }
}
