package com.x.order.entity;

public enum DiscountChannel {
    POS,
    ONLINE,
    BOTH;

    public boolean supports(OrderChannel orderChannel) {
        return this == BOTH
                || (this == POS && orderChannel == OrderChannel.POS)
                || (this == ONLINE
                && (orderChannel == OrderChannel.ONLINE || orderChannel == OrderChannel.MOBILE));
    }
}
