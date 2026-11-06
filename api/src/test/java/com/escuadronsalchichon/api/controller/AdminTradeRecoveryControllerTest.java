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
    void recoveryQueryExposesFingerprintsButNeverSelectsSerializedPayloads() {
        String query = AdminTradeRecoveryController.RECOVERY_QUERY;
        assertTrue(query.contains("o.item_fingerprint"));
        assertFalse(query.contains("o.item_payload"));
        assertTrue(query.contains("'PREPARED'"));
        assertTrue(query.contains("'DELIVERY_PENDING'"));
        assertTrue(query.contains("'DELIVERING'"));
        assertTrue(query.contains("'RETURN_PENDING'"));
        assertTrue(query.contains("'RECOVERY_REQUIRED'"));
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
