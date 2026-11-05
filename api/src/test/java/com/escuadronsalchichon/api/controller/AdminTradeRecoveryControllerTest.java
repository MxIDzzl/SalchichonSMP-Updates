package com.escuadronsalchichon.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AdminTradeRecoveryControllerTest {
    @Test
    void acceptsBoundedPagination() {
        assertDoesNotThrow(() -> AdminTradeRecoveryController.validatePagination(1, 0));
        assertDoesNotThrow(() -> AdminTradeRecoveryController.validatePagination(100, 100_000));
    }

    @Test
    void rejectsUnboundedOrInvalidPagination() {
        assertThrows(ResponseStatusException.class,
                () -> AdminTradeRecoveryController.validatePagination(0, 0));
        assertThrows(ResponseStatusException.class,
                () -> AdminTradeRecoveryController.validatePagination(101, 0));
        assertThrows(ResponseStatusException.class,
                () -> AdminTradeRecoveryController.validatePagination(50, -1));
        assertThrows(ResponseStatusException.class,
                () -> AdminTradeRecoveryController.validatePagination(50, 100_001));
    }
}
