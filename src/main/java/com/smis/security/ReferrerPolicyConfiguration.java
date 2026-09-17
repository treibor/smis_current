package com.smis.security;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration(proxyBeanMethods = false)
public class ReferrerPolicyConfiguration {

    @Bean
    FilterRegistrationBean<Filter> referrerPolicyFilter() {
        ReferrerPolicyHeaderWriter writer = new ReferrerPolicyHeaderWriter(
                ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
        // Write before filters that can short-circuit, and again on container error
        // dispatches (which may reset headers). This is the sole policy writer;
        // Spring's writer uses setHeader, so redispatches cannot append duplicates.
        Filter filter = (request, response, chain) -> {
            writer.writeHeaders((HttpServletRequest) request, (HttpServletResponse) response);
            chain.doFilter(request, response);
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("referrerPolicyFilter");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
        registration.setAsyncSupported(true);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
