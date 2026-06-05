package com.codex.finance.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedUserTest {

    @Test
    void getUserId_returnsSubClaim() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"), Map.of("sub", "user-123"));
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        AuthenticatedUser user = new AuthenticatedUser();
        assertEquals("user-123", user.getUserId());
    }

    @Test
    void getUserId_throwsWhenNoAuth() {
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(ctx);

        AuthenticatedUser user = new AuthenticatedUser();
        assertThrows(RuntimeException.class, user::getUserId);
    }

    @Test
    void getUserId_throwsWhenPrincipalNotJwt() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("not a jwt");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        AuthenticatedUser user = new AuthenticatedUser();
        assertThrows(RuntimeException.class, user::getUserId);
    }
}
