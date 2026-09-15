package com.smis.security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import com.smis.security.noauth.NoAuthTestAuthenticationFilter;

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
	private final ObjectProvider<NoAuthTestAuthenticationFilter> noAuthTestFilter;

	public SecurityConfiguration(ObjectProvider<NoAuthTestAuthenticationFilter> noAuthTestFilter) {
		this.noAuthTestFilter = noAuthTestFilter;
	}
	
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
        // Reject the same methods, after header writing is installed and before CSRF/authentication.
        .addFilterBefore(disableOptionsMethodFilter(), CsrfFilter.class)
        //.addFilterAfter(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
		.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(SecurityHeadersPolicy.CSP))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(false))
                .permissionsPolicy(permissions -> permissions.policy("geolocation=(self), microphone=()")))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.invalidSessionUrl("/")
						.sessionConcurrency(concurrency -> concurrency.maximumSessions(1).expiredUrl("/")
								.maxSessionsPreventsLogin(true) // Prevent new logins if the max sessions are reached
								.sessionRegistry(sessionRegistry())))
				.securityContext(context -> context.securityContextRepository(securityContextRepository())
						
		);
		http.authorizeHttpRequests(
				authorize -> authorize
                    // Container error dispatches retain their original 4xx/5xx status;
                    // direct client requests to /error still follow normal authorization.
                    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/security/purify.min.js", "GET"),
                            new AntPathRequestMatcher("/security/csp-registry.js", "GET"),
                            new AntPathRequestMatcher("/security/csp-compat.js", "GET")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());
		NoAuthTestAuthenticationFilter testFilter = noAuthTestFilter.getIfAvailable();
		if (testFilter != null) {
			http.addFilterBefore(testFilter, UsernamePasswordAuthenticationFilter.class);
		}
		super.configure(http);
		setLoginView(http, Login.class);
		//setLoginView(http, LoginView.class);
	}
}
