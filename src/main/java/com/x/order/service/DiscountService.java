package com.x.order.service;

import com.x.order.dto.DiscountRequest;
import com.x.order.dto.DiscountResponse;
import com.x.order.entity.*;
import com.x.order.repository.DiscountRedemptionRepository;
import com.x.order.repository.DiscountRepository;
import com.x.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiscountService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final Set<DiscountRedemptionStatus> USED_STATUSES =
            EnumSet.of(DiscountRedemptionStatus.RESERVED, DiscountRedemptionStatus.REDEEMED);

    private final DiscountRepository discountRepository;
    private final DiscountRedemptionRepository redemptionRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public DiscountResponse create(DiscountRequest request) {
        String code = normalizeCode(request.code());
        if (discountRepository.existsByBusinessIdAndCodeIgnoreCase(request.businessId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Discount code already exists");
        }
        Discount discount = Discount.builder()
                .businessId(request.businessId())
                .method(DiscountMethod.CODE)
                .build();
        applyRequest(discount, request, code);
        return toResponse(discountRepository.save(discount));
    }

    @Transactional
    public DiscountResponse update(Long id, Long businessId, DiscountRequest request) {
        if (!businessId.equals(request.businessId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business ID cannot be changed");
        }
        Discount discount = require(id, businessId);
        String code = normalizeCode(request.code());
        if (discountRepository.existsByBusinessIdAndCodeIgnoreCaseAndIdNot(businessId, code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Discount code already exists");
        }
        applyRequest(discount, request, code);
        return toResponse(discountRepository.save(discount));
    }

    @Transactional(readOnly = true)
    public DiscountResponse get(Long id, Long businessId) {
        return toResponse(require(id, businessId));
    }

    @Transactional(readOnly = true)
    public Page<DiscountResponse> list(Long businessId, int page, int size) {
        return discountRepository.findByBusinessId(
                        businessId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional
    public DiscountResponse deactivate(Long id, Long businessId) {
        Discount discount = require(id, businessId);
        discount.setStatus(DiscountStatus.INACTIVE);
        return toResponse(discountRepository.save(discount));
    }

    @Transactional
    public Order applyCodeAndSave(Order order, String rawCode, BigDecimal taxRate) {
        String code = normalizeCode(rawCode);
        Discount discount = discountRepository
                .findForUpdateByBusinessIdAndCodeIgnoreCase(order.getBusinessId(), code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Discount code is invalid"));

        validateEligibility(discount, order);
        if (!discount.isCombinable() && hasManualDiscounts(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Discount code cannot be combined with manual discounts");
        }
        validateUsage(discount, order.getCustomerId());
        applyDiscount(discount, order, taxRate);

        Order saved = orderRepository.save(order);
        redemptionRepository.save(DiscountRedemption.builder()
                .discount(discount)
                .order(saved)
                .customerId(saved.getCustomerId())
                .status(DiscountRedemptionStatus.RESERVED)
                .build());
        return saved;
    }

    @Transactional
    public void redeem(Long orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(redemption -> {
            if (redemption.getStatus() == DiscountRedemptionStatus.RESERVED) {
                redemption.setStatus(DiscountRedemptionStatus.REDEEMED);
                redemptionRepository.save(redemption);
            }
        });
    }

    @Transactional
    public void release(Long orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(redemption -> {
            if (redemption.getStatus() == DiscountRedemptionStatus.RESERVED) {
                redemption.setStatus(DiscountRedemptionStatus.RELEASED);
                redemptionRepository.save(redemption);
            }
        });
    }

    private void applyRequest(Discount discount, DiscountRequest request, String code) {
        validateDefinition(request);
        discount.setName(request.name().trim());
        discount.setCode(code);
        discount.setMethod(DiscountMethod.CODE);
        discount.setType(request.type());
        discount.setScope(request.scope());
        discount.setChannel(request.channel());
        discount.setValue(money(request.value()));
        discount.setCurrencyCode(request.type() == DiscountType.FIXED
                ? normalizeCurrency(request.currencyCode()) : null);
        discount.setMinimumOrderAmount(nullableMoney(request.minimumOrderAmount()));
        discount.setMaximumDiscountAmount(nullableMoney(request.maximumDiscountAmount()));
        discount.setStartsAt(request.startsAt());
        discount.setEndsAt(request.endsAt());
        discount.setUsageLimit(request.usageLimit());
        discount.setUsageLimitPerCustomer(request.usageLimitPerCustomer());
        discount.setCombinable(request.combinable());
        discount.setStatus(request.status() == null ? DiscountStatus.ACTIVE : request.status());
        discount.setStoreIds(copy(request.storeIds()));
        discount.setProductIds(copy(request.productIds()));
        discount.setVariantIds(copy(request.variantIds()));
    }

    private void validateDefinition(DiscountRequest request) {
        if (request.type() == DiscountType.PERCENTAGE
                && request.value().compareTo(ONE_HUNDRED) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        }
        if (request.type() == DiscountType.FIXED
                && (request.currencyCode() == null || request.currencyCode().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Currency code is required for a fixed discount");
        }
        if (request.startsAt() != null && request.endsAt() != null
                && !request.startsAt().isBefore(request.endsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Discount start must be before its end");
        }
        if (request.maximumDiscountAmount() != null
                && request.maximumDiscountAmount().signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Maximum discount amount must be greater than zero");
        }
        if (request.scope() == DiscountScope.ORDER
                && ((request.productIds() != null && !request.productIds().isEmpty())
                || (request.variantIds() != null && !request.variantIds().isEmpty()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product and variant targets are only allowed for item discounts");
        }
    }

    private void validateEligibility(Discount discount, Order order) {
        LocalDateTime now = LocalDateTime.now();
        if (discount.getStatus() != DiscountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount code is inactive");
        }
        if (discount.getStartsAt() != null && now.isBefore(discount.getStartsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount code has not started");
        }
        if (discount.getEndsAt() != null && !now.isBefore(discount.getEndsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount code has expired");
        }
        if (!discount.getChannel().supports(order.getOrderChannel())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Discount code is unavailable for this sales channel");
        }
        if (!discount.getStoreIds().isEmpty()
                && !discount.getStoreIds().contains(order.getStoreId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Discount code is unavailable for this store");
        }
        if (discount.getType() == DiscountType.FIXED
                && !discount.getCurrencyCode().equalsIgnoreCase(order.getCurrencyCode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Discount currency does not match the order currency");
        }
        if (discount.getMinimumOrderAmount() != null
                && order.getSubtotal().compareTo(discount.getMinimumOrderAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order does not meet the discount minimum amount");
        }
    }

    private void validateUsage(Discount discount, Long customerId) {
        long used = usedCount(discount.getId());
        if (discount.getUsageLimit() != null && used >= discount.getUsageLimit()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Discount code usage limit has been reached");
        }
        if (discount.getUsageLimitPerCustomer() != null) {
            if (customerId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Customer is required for this discount code");
            }
            long customerUsed = redemptionRepository
                    .countByDiscountIdAndCustomerIdAndStatusIn(
                            discount.getId(), customerId, USED_STATUSES);
            if (customerUsed >= discount.getUsageLimitPerCustomer()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Customer discount usage limit has been reached");
            }
        }
    }

    private void applyDiscount(Discount discount, Order order, BigDecimal taxRate) {
        BigDecimal totalDiscount;
        if (discount.getScope() == DiscountScope.ITEM) {
            totalDiscount = applyItemDiscount(discount, order, taxRate);
        } else {
            totalDiscount = calculate(
                    discount.getType(), discount.getValue(), order.getSubtotal(), null);
            totalDiscount = applyMaximum(totalDiscount, discount.getMaximumDiscountAmount());
            order.setDiscountType(discount.getType());
            order.setDiscountValue(discount.getValue());
            order.addDiscount(OrderDiscount.builder()
                    .discountId(discount.getId())
                    .name(discount.getName())
                    .code(discount.getCode())
                    .method(DiscountMethod.CODE)
                    .type(discount.getType())
                    .scope(DiscountScope.ORDER)
                    .value(discount.getValue())
                    .currencyCode(discount.getCurrencyCode())
                    .amount(totalDiscount)
                    .build());
            allocateOrderDiscount(order, totalDiscount, taxRate);
        }
        if (totalDiscount.signum() == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Discount code does not apply to an order item");
        }
        BigDecimal tax = money(order.getItems().stream()
                .map(OrderItem::getTaxAmount).reduce(ZERO, BigDecimal::add));
        order.setDiscount(totalDiscount);
        order.setTax(tax);
        order.setGrandTotal(money(order.getSubtotal().subtract(totalDiscount).add(tax)
                .add(order.getDeliveryFee() == null ? ZERO : order.getDeliveryFee())));
    }

    private BigDecimal applyItemDiscount(
            Discount discount, Order order, BigDecimal taxRate) {
        BigDecimal total = ZERO;
        BigDecimal remainingMaximum = discount.getMaximumDiscountAmount();
        boolean foundEligibleItem = false;

        for (OrderItem item : order.getItems()) {
            BigDecimal base = money(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())));
            if (!targetsItem(discount, item)) {
                item.setDiscountAmount(ZERO);
                item.setOrderDiscountAmount(ZERO);
                item.setTaxAmount(percentage(base, taxRate));
                item.setTotal(money(base.add(item.getTaxAmount())));
                continue;
            }
            foundEligibleItem = true;
            BigDecimal valueForLine = discount.getType() == DiscountType.FIXED
                    ? discount.getValue().multiply(BigDecimal.valueOf(item.getQty()))
                    : discount.getValue();
            BigDecimal amount = calculate(
                    discount.getType(), valueForLine, base, remainingMaximum);
            if (remainingMaximum != null) {
                remainingMaximum = remainingMaximum.subtract(amount);
            }
            if (amount.signum() > 0) {
                item.setDiscountType(discount.getType());
                item.setDiscountValue(discount.getValue());
                item.setDiscountAmount(amount);
                item.addDiscount(OrderItemDiscount.builder()
                        .discountId(discount.getId())
                        .name(discount.getName())
                        .code(discount.getCode())
                        .method(DiscountMethod.CODE)
                        .type(discount.getType())
                        .value(discount.getValue())
                        .currencyCode(discount.getCurrencyCode())
                        .amount(amount)
                        .build());
            }
            item.setOrderDiscountAmount(ZERO);
            item.setTaxAmount(percentage(base.subtract(amount), taxRate));
            item.setTotal(money(base.subtract(amount).add(item.getTaxAmount())));
            total = total.add(amount);
        }
        if (!foundEligibleItem) {
            return ZERO;
        }
        return money(total);
    }

    private void allocateOrderDiscount(
            Order order, BigDecimal orderDiscount, BigDecimal taxRate) {
        BigDecimal eligibleBase = order.getSubtotal();
        BigDecimal remaining = orderDiscount;
        for (int index = 0; index < order.getItems().size(); index++) {
            OrderItem item = order.getItems().get(index);
            BigDecimal base = money(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())));
            BigDecimal allocation = ZERO;
            if (orderDiscount.signum() > 0 && eligibleBase.signum() > 0) {
                allocation = index == order.getItems().size() - 1
                        ? remaining
                        : money(orderDiscount.multiply(base)
                        .divide(eligibleBase, 4, RoundingMode.HALF_UP));
            }
            allocation = allocation.min(base).min(remaining);
            remaining = remaining.subtract(allocation);
            item.setDiscountType(null);
            item.setDiscountValue(ZERO);
            item.setDiscountAmount(ZERO);
            item.setOrderDiscountAmount(allocation);
            item.setTaxAmount(percentage(base.subtract(allocation), taxRate));
            item.setTotal(money(base.subtract(allocation).add(item.getTaxAmount())));
        }
    }

    private BigDecimal calculate(
            DiscountType type, BigDecimal value, BigDecimal base, BigDecimal maximum) {
        BigDecimal amount = type == DiscountType.PERCENTAGE
                ? base.multiply(value).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP)
                : value;
        amount = amount.min(base);
        if (maximum != null) {
            amount = amount.min(maximum.max(ZERO));
        }
        return money(amount);
    }

    private BigDecimal applyMaximum(BigDecimal amount, BigDecimal maximum) {
        return maximum == null ? amount : money(amount.min(maximum));
    }

    private Discount require(Long id, Long businessId) {
        return discountRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Discount not found"));
    }

    private DiscountResponse toResponse(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getBusinessId(),
                discount.getName(),
                discount.getCode(),
                discount.getMethod(),
                discount.getType(),
                discount.getScope(),
                discount.getChannel(),
                discount.getValue(),
                discount.getCurrencyCode(),
                discount.getMinimumOrderAmount(),
                discount.getMaximumDiscountAmount(),
                discount.getStartsAt(),
                discount.getEndsAt(),
                discount.getUsageLimit(),
                discount.getUsageLimitPerCustomer(),
                usedCount(discount.getId()),
                discount.isCombinable(),
                discount.getStatus(),
                Set.copyOf(discount.getStoreIds()),
                Set.copyOf(discount.getProductIds()),
                Set.copyOf(discount.getVariantIds()),
                discount.getCreatedAt(),
                discount.getUpdatedAt());
    }

    private long usedCount(Long discountId) {
        return discountId == null ? 0
                : redemptionRepository.countByDiscountIdAndStatusIn(discountId, USED_STATUSES);
    }

    private BigDecimal percentage(BigDecimal base, BigDecimal rate) {
        return money(base.multiply(rate).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal nullableMoney(BigDecimal value) {
        return value == null ? null : money(value);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String code) {
        String value = code.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(value);
            return value;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid ISO 4217 currency code");
        }
    }

    private Set<Long> copy(Set<Long> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    private boolean targetsItem(Discount discount, OrderItem item) {
        boolean hasTargets = !discount.getProductIds().isEmpty()
                || !discount.getVariantIds().isEmpty();
        return !hasTargets
                || discount.getProductIds().contains(item.getProductId())
                || discount.getVariantIds().contains(item.getVariantId());
    }

    private boolean hasManualDiscounts(Order order) {
        if (order.getDiscounts().stream().anyMatch(d -> d.getMethod() == DiscountMethod.MANUAL)) {
            return true;
        }
        return order.getItems().stream()
                .flatMap(item -> item.getDiscounts().stream())
                .anyMatch(d -> d.getMethod() == DiscountMethod.MANUAL);
    }
}
