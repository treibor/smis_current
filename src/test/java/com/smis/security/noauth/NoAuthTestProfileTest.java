package com.smis.security.noauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.smis.entity.Users;
import com.smis.repository.UserRepository;
import com.smis.security.UserDetailsServiceImpl;

class NoAuthTestProfileTest {

	@Test
	void normalProfileDoesNotInstallTheAuthenticationBypass() {
		runner(mock(UserRepository.class), mock(UserDetailsServiceImpl.class)).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(NoAuthTestUser.class);
			assertThat(context).doesNotHaveBean(NoAuthTestAuthenticationFilter.class);
		});
	}

	@Test
	void noAuthProfileResolvesARealEnabledSuperUserWithNormalAuthorities() {
		Users databaseUser = databaseUser("local-super");
		User principal = principal("local-super");
		UserRepository repository = mock(UserRepository.class);
		UserDetailsServiceImpl detailsService = mock(UserDetailsServiceImpl.class);
		when(repository.findEnabledUsersWithRole("SUPER")).thenReturn(List.of(databaseUser));
		when(detailsService.loadUserByUsername("local-super")).thenReturn(principal);
		runner(repository, detailsService).withPropertyValues("spring.profiles.active=no-auth-test")
				.run(context -> {
					assertThat(context).hasNotFailed();
					NoAuthTestUser selected = context.getBean(NoAuthTestUser.class);
					assertThat(selected.getDatabaseUser()).isSameAs(databaseUser);
					assertThat(selected.getPrincipal()).isSameAs(principal);
					assertThat(selected.getPrincipal().getAuthorities())
							.extracting("authority").contains("ROLE_SUPER");
				});
	}

	@Test
	void productionCombinationFailsStartup() {
		runner(mock(UserRepository.class), mock(UserDetailsServiceImpl.class))
				.withPropertyValues("spring.profiles.active=no-auth-test,production")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasRootCauseMessage(
							"Profile 'no-auth-test' must never be combined with the 'prod' or 'production' profile.");
				});
	}

	@Test
	void missingSuperUserFailsStartupClearly() {
		UserRepository repository = mock(UserRepository.class);
		when(repository.findEnabledUsersWithRole("SUPER")).thenReturn(List.of());
		runner(repository, mock(UserDetailsServiceImpl.class))
				.withPropertyValues("spring.profiles.active=no-auth-test")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasRootCauseMessage(
							"Profile 'no-auth-test' requires an enabled database user with the SUPER role; none was found.");
				});
	}

	private static ApplicationContextRunner runner(UserRepository repository,
			UserDetailsServiceImpl detailsService) {
		return new ApplicationContextRunner().withUserConfiguration(NoAuthTestUser.class)
				.withBean(UserRepository.class, () -> repository)
				.withBean(UserDetailsServiceImpl.class, () -> detailsService);
	}

	private static Users databaseUser(String username) {
		Users user = new Users();
		user.setUserName(username);
		user.setEnabled(true);
		return user;
	}

	private static User principal(String username) {
		return new User(username, "database-password",
				java.util.List.of(new SimpleGrantedAuthority("ROLE_SUPER")));
	}
}
