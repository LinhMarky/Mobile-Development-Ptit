package com.homely.rental.auth;

import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.dto.response.LoginResponse;
import com.homely.rental.auth.repository.RefreshTokenRepository;
import com.homely.rental.auth.security.AccountAccessException;
import com.homely.rental.auth.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AdminAccountAuthTest {
    @Test void accessTokenIncludesDatabaseAdminRoleWithoutInventingTenantRole() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenAnswer(call -> {
            JwtEncoderParameters params = call.getArgument(0);
            return new Jwt("encoded", params.getClaims().getIssuedAt(), params.getClaims().getExpiresAt(),
                    params.getJwsHeader().getHeaders(), params.getClaims().getClaims());
        });
        TokenService service = new TokenService(encoder, mock(JwtDecoder.class), mock(RefreshTokenRepository.class), mock(com.homely.rental.auth.security.AccountAccessService.class));
        ReflectionTestUtils.setField(service, "accessTokenExpiration", 900L);
        User user = new User(); user.setId(1L); user.setEmail("admin@example.test"); user.setFullName("Admin");
        Role role = new Role(); role.setName(RoleName.ROLE_ADMIN); user.getRoles().add(role);
        LoginResponse login = new LoginResponse(); login.setUser(UserService.convertToUserLogin(user));
        service.createAccessToken(user.getEmail(), login);
        var capture = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(capture.capture());
        assertThat(capture.getValue().getClaims().getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");
        assertThat(capture.getValue().getClaims().getClaimAsString("token_type")).isEqualTo("access");
    }

    @Test void legacySuspendedFlagBlocksPasswordAuthentication() {
        UserService users = mock(UserService.class);
        User user = new User(); user.setSuspended(true);
        when(users.handleGetUserByUsername("user@example.test")).thenReturn(user);
        assertThatThrownBy(() -> new CustomUserDetailsService(users).loadUserByUsername("user@example.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test void legacySuspendedFlagBlocksRefreshEvenBeforeSessionRevocation() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        RefreshTokenRepository sessions = mock(RefreshTokenRepository.class);
        when(decoder.decode("refresh")).thenReturn(Jwt.withTokenValue("refresh").header("alg", "HS256")
                .subject("user@example.test").claim("token_type", "refresh")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build());
        User user = new User(); user.setSuspended(true);
        RefreshToken session = new RefreshToken(); session.setUser(user); session.setExpiresAt(Instant.now().plusSeconds(600));
        when(sessions.findByTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.of(session));
        TokenService service = new TokenService(mock(JwtEncoder.class), decoder, sessions, mock(com.homely.rental.auth.security.AccountAccessService.class));
        assertThatThrownBy(() -> service.validateRefreshToken("refresh"))
                .isInstanceOfSatisfying(AccountAccessException.class, error -> assertThat(error.getCode()).isEqualTo("ACCOUNT_INACTIVE"));
    }
}
