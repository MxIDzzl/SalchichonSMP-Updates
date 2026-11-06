package com.escuadronsalchichon.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthInterceptorTest {
    @Test
    void rejectsRequestsWhenTokenIsNotConfigured() throws Exception {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor("");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(503, response.getStatus());
    }

    @Test
    void rejectsMissingAndIncorrectBearerTokens() throws Exception {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor("a".repeat(40));
        MockHttpServletRequest missing = new MockHttpServletRequest();
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(missing, missingResponse, new Object()));
        assertEquals(401, missingResponse.getStatus());

        MockHttpServletRequest incorrect = new MockHttpServletRequest();
        incorrect.addHeader("Authorization", "Bearer " + "b".repeat(40));
        MockHttpServletResponse incorrectResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(incorrect, incorrectResponse, new Object()));
        assertEquals(401, incorrectResponse.getStatus());
    }

    @Test
    void acceptsExactConfiguredBearerToken() throws Exception {
        String token = "c".repeat(48);
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(token);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }
}
