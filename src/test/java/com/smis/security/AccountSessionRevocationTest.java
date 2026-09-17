package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.smis.dbservice.Dbservice;
import com.smis.entity.Users;
import com.smis.repository.UserRepository;

class AccountSessionRevocationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SessionRegistryImpl registry = new SessionRegistryImpl();
    private final Dbservice service = new Dbservice(null, users, null, null, null, null, null, null,
            null, null, null, null, null, null, null);

    AccountSessionRevocationTest() {
        ReflectionTestUtils.setField(service, "sessionRegistry", registry);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cleanup() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    @Test
    void disablingAccountRevokesAllItsSessionsAfterCommitAndBlocksNextRequest() throws Exception {
        var principal = User.withUsername("disabled-user").password("hash").roles("USER").build();
        var other = User.withUsername("other-user").password("hash").roles("USER").build();
        var request = new MockHttpServletRequest("POST", "/");
        var session = request.getSession();
        registry.registerNewSession(session.getId(), principal);
        registry.registerNewSession("second-session", principal);
        registry.registerNewSession("unrelated-session", other);
        Users account = account(false);

        service.saveUser(account);

        verify(users).save(account);
        assertFalse(registry.getSessionInformation(session.getId()).isExpired());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertTrue(registry.getSessionInformation(session.getId()).isExpired());
        assertTrue(registry.getSessionInformation("second-session").isExpired());
        assertFalse(registry.getSessionInformation("unrelated-session").isExpired());

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        var response = new MockHttpServletResponse();
        var reachedApplication = new AtomicBoolean();
        new ConcurrentSessionFilter(registry, event -> event.getResponse().sendRedirect("/login"))
                .doFilter(request, response, (req, res) -> reachedApplication.set(true));
        assertFalse(reachedApplication.get());
        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/login", response.getRedirectedUrl());
    }

    @Test
    void rollbackDoesNotRevokeSessions() {
        var principal = User.withUsername("disabled-user").password("hash").roles("USER").build();
        registry.registerNewSession("active", principal);
        service.saveUser(account(false));
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertFalse(registry.getSessionInformation("active").isExpired());
    }

    @Test
    void failedSaveDoesNotScheduleRevocation() {
        doThrow(new IllegalStateException("database failure")).when(users).save(any());
        assertThrows(IllegalStateException.class, () -> service.saveUser(account(false)));
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void savingEnabledAccountDoesNotScheduleRevocation() {
        service.saveUser(account(true));
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    private Users account(boolean enabled) {
        var account = new Users();
        account.setUserName("disabled-user");
        account.setEnabled(enabled);
        return account;
    }
}
