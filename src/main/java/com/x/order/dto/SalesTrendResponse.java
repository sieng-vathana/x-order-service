package com.x.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesTrendResponse(
        LocalDate date, long orderCount, BigDecimal grandTotal) {
}
