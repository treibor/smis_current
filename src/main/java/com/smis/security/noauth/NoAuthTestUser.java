package com.smis.security.noauth;

import java.util.Arrays;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.smis.entity.Users;
import com.smis.repository.UserRepository;
import com.smis.security.UserDetailsServiceImpl;

/** Resolves the real database-backed identity used by the local test profile. */
@Component
@Profile(NoAuthTestUser.PROFILE)
public class NoAuthTestUser {
	public static final String PROFILE = "no-auth-test";
	public static final String SUPER_ROLE = "SUPER";

	private final Users databaseUser;
	private final UserDetails principal;

	public NoAuthTestUser(Environment environment, UserRepository userRepository,
			UserDetailsServiceImpl userDetailsService) {
		assertNotProduction(environment);
		this.databaseUser = userRepository.findEnabledUsersWithRole(SUPER_ROLE).stream().findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Profile 'no-auth-test' requires an enabled database user with the SUPER role; none was found."));
		this.principal = userDetailsService.loadUserByUsername(databaseUser.getUserName());
		boolean hasSuperAuthority = principal.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_SUPER".equalsIgnoreCase(authority.getAuthority()));
		if (!hasSuperAuthority) {
			throw new IllegalStateException("Profile 'no-auth-test' resolved user '" + databaseUser.getUserName()
					+ "' without the required ROLE_SUPER authority.");
		}
	}

	private static void assertNotProduction(Environment environment) {
		boolean production = Arrays.stream(environment.getActiveProfiles())
				.anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
		if (production) {
			throw new IllegalStateException(
					"Profile 'no-auth-test' must never be combined with the 'prod' or 'production' profile.");
		}
	}

	public Users getDatabaseUser() { return databaseUser; }
	public UserDetails getPrincipal() { return principal; }
}
