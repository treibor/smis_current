package com.smis.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.vaadin.flow.server.auth.ViewAccessChecker;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = ReferrerPolicyTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReferrerPolicyTest {
    private static final String POLICY = "strict-origin-when-cross-origin";
    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate http;

    @Test
    void publicStaticGetAndHead() throws Exception {
        exact(mvc.perform(get("/images/plant.png")).andExpect(status().isOk()));
        exact(mvc.perform(head("/images/plant.png")).andExpect(status().isOk()));
    }

    @Test
    void missingStaticResource() throws Exception {
        exact(mvc.perform(head("/images/plant-16x16.png")).andExpect(status().isNotFound()));
    }

    @Test
    void authenticatedAndApiResponses() throws Exception {
        exact(mvc.perform(get("/api/header-probe").with(user("tester"))).andExpect(status().isOk()));
        exact(mvc.perform(get("/api/header-probe")).andExpect(status().is3xxRedirection()));
    }

    @Test
    void securityAndEarlyFilterErrors() throws Exception {
        exact(mvc.perform(post("/api/header-probe").with(user("tester"))).andExpect(status().is3xxRedirection()));
        exact(mvc.perform(get("/api/denied").with(user("tester"))).andExpect(status().isForbidden()));
        exact(mvc.perform(put("/images/plant.png")).andExpect(status().isMethodNotAllowed()));
    }

    @Test
    void realContainerErrorDispatches() throws Exception {
        // /login is already public in the production chain. The test-only
        // controller throws there to exercise an actual Tomcat ERROR dispatch.
        var failure = http.getForEntity("/login", String.class);
        assertThat(failure.getStatusCode().value()).isEqualTo(500);
        assertThat(failure.getHeaders().get("Referrer-Policy")).containsExactly(POLICY);
        assertThat(failure.getHeaders().get("Content-Security-Policy")).containsExactly(SecurityHeadersPolicy.CSP);
        assertThat(failure.getHeaders().get("X-Frame-Options")).containsExactly("DENY");
        assertThat(failure.getHeaders().get("X-Content-Type-Options")).containsExactly("nosniff");
        assertThat(failure.getHeaders().get("X-XSS-Protection")).containsExactly("0");
        assertThat(failure.getHeaders()).doesNotContainKeys("Expect-CT", "Content-Security-Policy-Report-Only");
        var missing = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(http.getRootUri() + "/images/plant-16x16.png"))
                .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody()).build(),
                java.net.http.HttpResponse.BodyHandlers.discarding());
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.headers().allValues("Referrer-Policy")).containsExactly(POLICY);
        assertThat(missing.headers().allValues("Content-Security-Policy")).containsExactly(SecurityHeadersPolicy.CSP);
    }

    private void exact(ResultActions response) throws Exception {
        response.andExpect(header().string("Referrer-Policy", POLICY))
                .andExpect(header().string("Content-Security-Policy", SecurityHeadersPolicy.CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "0"))
                .andExpect(header().string("Permissions-Policy", "geolocation=(self), microphone=()"))
                .andExpect(header().doesNotExist("Content-Security-Policy-Report-Only"))
                .andExpect(header().doesNotExist("Expect-CT"))
                .andExpect(result -> {
                    for (String name : List.of("Content-Security-Policy", "X-Frame-Options", "X-Content-Type-Options", "X-XSS-Protection")) {
                        assertThat(result.getResponse().getHeaders(name)).hasSize(1);
                    }
                    assertThat(result.getResponse().getHeaders("Set-Cookie"))
                            .noneMatch(cookie -> cookie.startsWith("SameSite="));
                })
                .andExpect(result -> assertThat(result.getResponse().getHeaders("Referrer-Policy"))
                        .containsExactly(POLICY));
    }

    @Test
    void hstsOnlyOnSecureRequestsAndPublicCachingIsPreserved() throws Exception {
        mvc.perform(get("/images/plant.png")).andExpect(header().doesNotExist("Strict-Transport-Security"));
        mvc.perform(get("/images/plant.png").secure(true))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000"));
        exact(mvc.perform(get("/images/cache-probe.png")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable")));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({SecurityConfiguration.class, ReferrerPolicyConfiguration.class, Endpoints.class})
    @ImportAutoConfiguration({org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.class,
            org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
            ErrorMvcAutoConfiguration.class, SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
    static class Application {
        @Bean RateLimitingFilter rateLimitingFilter() { return new RateLimitingFilter(); }
        @Bean static org.springframework.beans.factory.config.BeanFactoryPostProcessor vaadinMocks() {
            return factory -> {
            factory.registerSingleton("viewAccessChecker", mock(ViewAccessChecker.class));
            factory.registerSingleton("requestCache", mock(VaadinDefaultRequestCache.class));
            factory.registerSingleton("requestUtil", mock(RequestUtil.class, invocation -> {
                if (invocation.getMethod().getName().equals("getUrlMapping")) return "/*";
                if (invocation.getMethod().getName().equals("applyUrlMapping")) {
                    String path = invocation.getArgument(0);
                    return path.isEmpty() ? "/" : path;
                }
                return RETURNS_DEFAULTS.answer(invocation);
            }));
            };
        }
        @Bean InMemoryUserDetailsManager users() {
            return new InMemoryUserDetailsManager(List.of(
                    User.withUsername("tester").password(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("unused")).roles("USER").build()));
        }
    }

    @RestController
    static class Endpoints {
        @GetMapping("/images/cache-probe.png") String cache(jakarta.servlet.http.HttpServletResponse response) {
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
            return "cache test";
        }
        @GetMapping("/images/session.png") String session(jakarta.servlet.http.HttpServletRequest request) {
            request.getSession();
            return ((org.springframework.security.web.csrf.CsrfToken) request.getAttribute(
                    org.springframework.security.web.csrf.CsrfToken.class.getName())).getToken();
        }
        @GetMapping("/api/header-probe") String api() { return "ok"; }
        @GetMapping("/api/denied") String denied() {
            throw new org.springframework.security.access.AccessDeniedException("test denial");
        }
        @GetMapping("/images/error.png") void error(jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response) {
            response.setStatus((Integer) request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE));
        }
        @GetMapping("/login") String failure() { throw new IllegalStateException("test error dispatch"); }
    }
}
