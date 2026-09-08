package com.x.order.controller;

import com.sharedlib.response.ApiResponse;
import com.sharedlib.response.PageResponse;
import com.x.order.dto.*;
import com.x.order.entity.FulfillmentType;
import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderStatus;
import com.x.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/pos")
    public ResponseEntity<ApiResponse<OrderResponse>> createPos(@Valid @RequestBody CreatePosOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "POS order created", orderService.createPos(request)));
    }

    @PostMapping("/holds")
    public ResponseEntity<ApiResponse<OrderResponse>> createHeld(
            @Valid @RequestBody CreateHeldPosOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "POS sale held", orderService.createHeld(request)));
    }

    @GetMapping("/holds")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> listHeld(
            @RequestParam @Positive Long storeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        Page<OrderResponse> result = orderService.listHeld(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(200, new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext())));
    }

    @PostMapping("/holds/{id}/resume")
    public ResponseEntity<ApiResponse<OrderResponse>> resumeHeld(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "Held sale resumed", orderService.resumeHeld(id)));
    }

    @PostMapping("/holds/{id}/discard")
    public ResponseEntity<ApiResponse<OrderResponse>> discardHeld(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "Held sale discarded", orderService.discardHeld(id)));
    }

    @PostMapping("/online")
    public ResponseEntity<ApiResponse<OrderResponse>> createOnline(@Valid @RequestBody CreateOnlineOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Online order created", orderService.createOnline(request)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOnline(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Online order confirmed", orderService.confirmOnline(id)));
    }

    @PostMapping("/{id}/pack")
    public ResponseEntity<ApiResponse<OrderResponse>> pack(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Order marked as packing", orderService.pack(id)));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> ship(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Order marked as shipped", orderService.ship(id)));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliver(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                "Order marked as delivered", orderService.deliver(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> get(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, orderService.get(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> list(
            @RequestParam @Positive Long storeId,
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) FulfillmentType fulfillmentType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<OrderResponse> result = orderService.list(storeId, channel, status, fulfillmentType, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(200, new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext())));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "Order completed", orderService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(200, "Order cancelled", orderService.cancel(id)));
    }

    @GetMapping("/reports/sales-summary")
    public ResponseEntity<ApiResponse<SalesSummaryResponse>> salesSummary(
            @RequestParam @Positive Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(200, orderService.salesSummary(storeId, from, to)));
    }

    @GetMapping("/reports/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> topProducts(
            @RequestParam @Positive Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(ApiResponse.success(200, orderService.topProducts(storeId, from, to, limit)));
    }

    @GetMapping("/reports/sales-trend")
    public ResponseEntity<ApiResponse<List<SalesTrendResponse>>> salesTrend(
            @RequestParam @Positive Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(200, orderService.salesTrend(storeId, from, to)));
    }
}
