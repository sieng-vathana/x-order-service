package com.x.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportOffsetTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 19, 17, 0);

    @Test
    void phnomPenhIsSevenHoursAheadOfStoredUtcTimes() {
        assertEquals(420, OrderService.offsetMinutes("Asia/Phnom_Penh", FROM));
    }

    @Test
    void noTimeZoneKeepsUtc() {
        assertEquals(0, OrderService.offsetMinutes(null, FROM));
        assertEquals(0, OrderService.offsetMinutes(" ", FROM));
    }

    @Test
    void unknownTimeZoneIsABadRequest() {
        assertThrows(ResponseStatusException.class, () -> OrderService.offsetMinutes("Mars/Base", FROM));
    }
}
