package com.smis.security.noauth;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Authenticates each new local test session with the selected real SUPER user. */
@Component
@Profile(NoAuthTestUser.PROFILE)
public class NoAuthTestAuthenticationFilter extends OncePerRequestFilter {
	private final NoAuthTestUser testUser;
	private final SecurityContextRepository securityContextRepository;

	public NoAuthTestAuthenticationFilter(NoAuthTestUser testUser,
			SecurityContextRepository securityContextRepository) {
		this.testUser = testUser;
		this.securityContextRepository = securityContextRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication current = SecurityContextHolder.getContext().getAuthentication();
		if (current == null || current instanceof AnonymousAuthenticationToken || !current.isAuthenticated()) {
			UserDetails principal = testUser.getPrincipal();
			Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal,
					principal.getPassword(), principal.getAuthorities());
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, request, response);
		}
		filterChain.doFilter(request, response);
	}
}
