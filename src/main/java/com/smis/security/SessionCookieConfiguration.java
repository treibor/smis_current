package com.smis.security;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Servlet 6 cookie settings apply to both embedded and external Tomcat 10.1. */
@Configuration(proxyBeanMethods = false)
public class SessionCookieConfiguration {
    @Bean
    ServletContextInitializer sessionCookieInitializer(Environment environment) {
        return context -> {
            var cookie = context.getSessionCookieConfig();
            cookie.setHttpOnly(true);
            cookie.setSecure(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, true));
            cookie.setAttribute("SameSite", "Strict");
            cookie.setPath(context.getContextPath().isEmpty() ? "/" : context.getContextPath());
        };
    }
}
