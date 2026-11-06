package com.escuadronsalchichon.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminAnnouncementControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void auditDetailsAreSerializedAsValidJsonIncludingStringValues() throws Exception {
        String json = AdminAnnouncementController.serializeAuditDetails(
                Map.of("published", true, "category", "UPDATE"), mapper);

        var parsed = mapper.readTree(json);
        assertTrue(parsed.get("published").asBoolean());
        assertEquals("UPDATE", parsed.get("category").asText());
    }

    @Test
    void auditDetailsEscapeQuotesAndUnicodeCorrectly() throws Exception {
        String json = AdminAnnouncementController.serializeAuditDetails(
                Map.of("title", "Bocchi: \"¡En vivo!\" 🎸"), mapper);

        assertEquals("Bocchi: \"¡En vivo!\" 🎸", mapper.readTree(json).get("title").asText());
    }
}
