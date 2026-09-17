package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterTest {
    private final RateLimitingFilter filter = new RateLimitingFilter();

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void anonymousClientsHaveSeparateAllowancesAndForwardedHeadersCannotResetThem() throws Exception {
        for (int i = 0; i < 50; i++) assertEquals(200, send(filter, "/login", "192.0.2.1", null).getStatus());
        var rejected = send(filter, "/login", "192.0.2.1", null);
        assertEquals(429, rejected.getStatus());
        assertEquals("60", rejected.getHeader("Retry-After"));
        assertEquals("no-store", rejected.getHeader("Cache-Control"));
        assertEquals(200, send(filter, "/login", "192.0.2.2", null).getStatus());
        var request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "192.0.2.99");
        request.addHeader("Forwarded", "for=192.0.2.99");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Exhausted client reached application"));
        assertEquals(429, response.getStatus());
    }

    @Test
    void authenticatedUsersBehindSameIpHaveSeparateAllowances() throws Exception {
        for (int i = 0; i < 10; i++) assertEquals(200, send(filter, "/dashboard", "192.0.2.1", "alice").getStatus());
        assertEquals(429, send(filter, "/dashboard", "192.0.2.1", "alice").getStatus());
        assertEquals(200, send(filter, "/dashboard", "192.0.2.1", "bob").getStatus());
        assertEquals(429, send(filter, "/dashboard", "192.0.2.2", "alice").getStatus());
        assertEquals(200, send(filter, "/", "192.0.2.1", "alice").getStatus());
    }

    @Test
    void contextPathAndPathVariantsDoNotCreateUnlimitedBuckets() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, send(filter, "/smis/variant" + i + "/dashboard", "192.0.2.1", null).getStatus());
        }
        assertEquals(429, send(filter, "/smis/dashboard", "192.0.2.1", null).getStatus());
        assertEquals(200, send(filter, "/smis/images/plant.png", "192.0.2.1", null).getStatus());
    }

    @Test
    void idleEntriesExpireAndCapacityDoesNotEvictActiveLimits() throws Exception {
        var now = new AtomicLong();
        var bounded = new RateLimitingFilter(now::get, 1);
        for (int i = 0; i < 10; i++) send(bounded, "/dashboard", "192.0.2.1", null);
        assertEquals(429, send(bounded, "/dashboard", "192.0.2.2", null).getStatus());
        assertEquals(429, send(bounded, "/dashboard", "192.0.2.1", null).getStatus());
        now.addAndGet(Duration.ofMinutes(11).toNanos());
        assertEquals(200, send(bounded, "/dashboard", "192.0.2.2", null).getStatus());
    }

    private MockHttpServletResponse send(RateLimitingFilter limiter, String path, String ip, String user) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        if (path.startsWith("/smis/")) request.setContextPath("/smis");
        request.setRemoteAddr(ip);
        SecurityContextHolder.clearContext();
        if (user != null) SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, java.util.List.of()));
        var response = new MockHttpServletResponse();
        limiter.doFilter(request, response, (req, res) -> res.getWriter().write("application"));
        if (response.getStatus() == 429) assertFalse(response.getContentAsString().contains("application"));
        return response;
    }
}
