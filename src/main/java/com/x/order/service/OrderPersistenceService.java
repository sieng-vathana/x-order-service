package com.x.order.service;

import com.x.order.entity.*;
import com.x.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {
    private final OrderRepository orderRepository;

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public Order attachReservation(Long orderId, Long itemId, Long reservationId) {
        Order order = require(orderId);
        OrderItem item = order.getItems().stream().filter(value -> value.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));
        item.setReservationId(reservationId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markCompleted(Long orderId) {
        Order order = require(orderId);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOnline(Long orderId) {
        Order order = require(orderId);
        if (order.getOrderChannel() != OrderChannel.ONLINE
                || order.getOrderStatus() != OrderStatus.PENDING_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not awaiting online confirmation");
        }
        order.setOrderStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markPacking(Long orderId) {
        Order order = require(orderId);
        if (order.getOrderStatus() != OrderStatus.CONFIRMED && order.getOrderStatus() != OrderStatus.PENDING_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be marked packing from status " + order.getOrderStatus());
        }
        order.setOrderStatus(OrderStatus.PACKING);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markShipped(Long orderId) {
        Order order = require(orderId);
        if (order.getOrderStatus() != OrderStatus.PACKING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be marked shipped from status " + order.getOrderStatus());
        }
        order.setOrderStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markDelivered(Long orderId) {
        Order order = require(orderId);
        if (order.getOrderStatus() != OrderStatus.SHIPPED && order.getOrderStatus() != OrderStatus.PACKING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be marked delivered from status " + order.getOrderStatus());
        }
        order.setOrderStatus(OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markCancelled(Long orderId) {
        Order order = require(orderId);
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A completed order cannot be cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteDraft(Long orderId) {
        Order order = require(orderId);
        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Held sale is no longer active");
        }
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public Order require(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        initializeDiscounts(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        Optional<Order> order = orderRepository.findByIdempotencyKey(idempotencyKey);
        order.ifPresent(this::initializeDiscounts);
        return order;
    }

    private void initializeDiscounts(Order order) {
        order.getDiscounts().size();
        order.getItems().forEach(item -> item.getDiscounts().size());
    }
}
