package com.homely.rental.auth;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.auth.security.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserResolverTest {
    UserRepository users = mock(UserRepository.class);
    UserResolver resolver = new UserResolver(new AccountAccessService(users,
            org.mockito.Mockito.mock(com.homely.rental.auth.repository.AccountDeletionRequestRepository.class),
            org.mockito.Mockito.mock(jakarta.persistence.EntityManager.class),
            org.mockito.Mockito.mock(com.homely.rental.auth.service.AccountLifecycleGuard.class)));
    User user;

    @BeforeEach void fixture() {
        user = new User(); user.setEmail("member@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), "unused", List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void returnsDatabaseAccountAndEnforcesLiveSuspension() {
        assertThat(resolver.requireCurrent()).isSameAs(user);
        user.setSuspended(true);
        assertThatThrownBy(resolver::requireCurrent).isInstanceOfSatisfying(AccountAccessException.class,
                error -> assertThat(error.getStatus()).isEqualTo(403));
    }
    @Test void deletedAccountIsRejected() {
        user.setStatus(UserStatus.DELETED);
        assertThatThrownBy(resolver::requireCurrent).isInstanceOf(AccountAccessException.class);
    }
    @Test void missingDatabaseAccountRequiresAuthentication() {
        when(users.findByEmail(user.getEmail())).thenReturn(null);
        assertThatThrownBy(resolver::requireCurrent).isInstanceOfSatisfying(AccountAccessException.class,
                error -> assertThat(error.getStatus()).isEqualTo(401));
    }
    @Test void anonymousAuthenticationCannotResolveAnAnonymousUserRow() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(resolver.currentIfAuthenticated()).isEmpty();
        assertThatThrownBy(resolver::requireCurrent).isInstanceOf(AccountAccessException.class);
        verify(users, never()).findByEmail("anonymousUser");
    }
    @Test void unauthenticatedPrincipalCannotResolveAnAccount() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), "unused"));
        assertThat(resolver.currentIfAuthenticated()).isEmpty();
        assertThatThrownBy(resolver::requireCurrent).isInstanceOf(AccountAccessException.class);
    }
    @Test void publicReadAllowsNoAccountButRejectsSuspendedAuthenticatedAccount() {
        user.setSuspended(true);
        assertThatThrownBy(resolver::currentIfAuthenticated).isInstanceOf(AccountAccessException.class);
        SecurityContextHolder.clearContext(); assertThat(resolver.currentIfAuthenticated()).isEmpty();
    }
    @Test void verifiedAndHostRequirementsAreIndependent() {
        assertThatThrownBy(resolver::requireVerified).isInstanceOf(AccountAccessException.class);
        user.setEmailVerified(true); assertThat(resolver.requireVerified()).isSameAs(user);
        assertThatThrownBy(resolver::requireHost).isInstanceOf(AccountAccessException.class);
        Role role = new Role(); role.setName(RoleName.ROLE_HOST); user.getRoles().add(role);
        assertThat(resolver.requireHost()).isSameAs(user);
    }
    @Test void adminRequiresDatabaseRole() {
        assertThatThrownBy(resolver::requireAdmin).isInstanceOf(AccountAccessException.class);
        Role role = new Role(); role.setName(RoleName.ROLE_ADMIN); user.getRoles().add(role);
        assertThat(resolver.requireAdmin()).isSameAs(user);
    }
}
