package com.smis.security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//@EnableWebSecurity

@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {
	@Autowired
	private RateLimitingFilter rateLimitingFilter;
	
	@Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "PUT", "POST"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
	

	   
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
		return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	@Bean
	public ConcurrentSessionControlAuthenticationStrategy concurrentSessionControlAuthenticationStrategy() {
		ConcurrentSessionControlAuthenticationStrategy strategy = new ConcurrentSessionControlAuthenticationStrategy(
				sessionRegistry());
		strategy.setMaximumSessions(1); // Allow only one session per user
		strategy.setExceptionIfMaximumExceeded(true); // Prevent new logins if maximum sessions are reached 
		return strategy;
	}

	@Bean
	Filter disableOptionsMethodFilter() {
		return new Filter() {

			@Override
			public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
					throws IOException, ServletException {
				HttpServletRequest request = (HttpServletRequest) req;
				HttpServletResponse response = (HttpServletResponse) res;
				String method = request.getMethod();
				if ("OPTIONS".equals(method) || "DELETE".equals(method) || "PATCH".equals(method)
						|| "PUT".equals(method) || "PROPFIND".equals(method) || "PROPPATCH".equals(method)
						|| "MKCOL".equals(method) || "COPY".equals(method) || "MOVE".equals(method)
						|| "LOCK".equals(method) || "UNLOCK".equals(method)) {
					response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				} else {
					chain.doFilter(req, res);
				}
			}
		};
	}



	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
		//.addFilterBefore(rateLimitingFilter, ChannelProcessingFilter.class)
        .addFilterBefore(disableOptionsMethodFilter(), ChannelProcessingFilter.class)
        //.addFilterAfter(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
		.headers(headers -> headers
				.addHeaderWriter(new StaticHeadersWriter("Strict-Transport-Security", "max-age=31536000"))
	            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
	            .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
	            .addHeaderWriter(new StaticHeadersWriter("X-Frame-Options", "DENY"))
	            .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
	            //.addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; object-src 'none';"))
	            .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=(self), microphone=()"))
	            .addHeaderWriter(new StaticHeadersWriter("Set-Cookie", "SameSite=Strict; HttpOnly; Secure;"))
	            .addHeaderWriter(new StaticHeadersWriter("Expect-CT", "max-age=86400, enforce"))
	            .addHeaderWriter(new StaticHeadersWriter("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0"))
	            .addHeaderWriter(new StaticHeadersWriter("Pragma", "no-cache"))
	            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN)))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.invalidSessionUrl("/")
						.sessionConcurrency(concurrency -> concurrency.maximumSessions(1).expiredUrl("/")
								.maxSessionsPreventsLogin(true) // Prevent new logins if the max sessions are reached
								.sessionRegistry(sessionRegistry())))
				.securityContext(context -> context.securityContextRepository(securityContextRepository())
						
		);
		http.authorizeHttpRequests(
				authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());
		super.configure(http);
		setLoginView(http, Login.class);
		//setLoginView(http, LoginView.class);
	}
}