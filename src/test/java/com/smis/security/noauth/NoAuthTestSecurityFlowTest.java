package com.smis.security.noauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.smis.entity.Users;
import com.smis.repository.UserRepository;
import com.smis.security.AuthenticatedUser;
import com.smis.view.SuperMasterView;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.spring.security.AuthenticationContext;

class NoAuthTestSecurityFlowTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void filterEstablishesSessionAuthenticationAndCurrentUserStillResolvesDatabaseEntity() throws Exception {
		UserDetails principal = User.withUsername("local-super").password("database-password").roles("SUPER").build();
		NoAuthTestUser selected = mock(NoAuthTestUser.class);
		when(selected.getPrincipal()).thenReturn(principal);
		HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
		NoAuthTestAuthenticationFilter filter = new NoAuthTestAuthenticationFilter(selected, contextRepository);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/supermaster");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(principal);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
				.extracting("authority").contains("ROLE_SUPER");
		assertThat(contextRepository.loadDeferredContext(request).get().getAuthentication().getName())
				.isEqualTo("local-super");

		Users databaseUser = new Users();
		databaseUser.setUserName("local-super");
		AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
		UserRepository userRepository = mock(UserRepository.class);
		when(authenticationContext.getAuthenticatedUser(UserDetails.class)).thenReturn(Optional.of(principal));
		when(userRepository.findByUserName("local-super")).thenReturn(databaseUser);
		assertThat(new AuthenticatedUser(authenticationContext, userRepository).get()).containsSame(databaseUser);
	}

	@Test
	void normalSuperAuthorityPassesProtectedVaadinViewAccessCheck() {
		UserDetails principal = User.withUsername("local-super").password("database-password").roles("SUPER").build();
		AccessAnnotationChecker checker = new AccessAnnotationChecker();
		assertThat(checker.hasAccess(SuperMasterView.class, () -> principal.getUsername(),
				role -> principal.getAuthorities().contains(new org.springframework.security.core.authority.SimpleGrantedAuthority(
						"ROLE_" + role)))).isTrue();
		assertThat(principal.getAuthorities()).extracting("authority").isEqualTo(List.of("ROLE_SUPER"));
	}
}
