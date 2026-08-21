package com.x.order.repository;

import java.math.BigDecimal;
import java.sql.Date;

public interface SalesTrendProjection {
    Date getSalesDate();
    Long getOrderCount();
    BigDecimal getGrandTotal();
}
