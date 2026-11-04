package com.emeal.security;

import com.emeal.config.BusinessTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUsername_throwsWhenNoAuthentication_neverFallsBackToAdmin() {
        SecurityContextHolder.clearContext();
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUsername);
    }

    @Test
    void getCurrentUsername_rejectsAnonymousToken() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUsername);
        assertFalse(SecurityUtils.isUserRole());
    }

    @Test
    void getCurrentUsername_rejectsUnauthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "pw")); // not authenticated
        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUsername);
    }

    @Test
    void getCurrentUsername_andRole_fromAuthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertEquals("alice", SecurityUtils.getCurrentUsername());
        assertEquals("USER", SecurityUtils.getCurrentRole());
        assertTrue(SecurityUtils.isUserRole());
    }

    @Test
    void businessToday_isKigaliDate() {
        LocalDate before = LocalDate.now(ZoneId.of("Africa/Kigali"));
        LocalDate today = BusinessTime.today();
        LocalDate after = LocalDate.now(ZoneId.of("Africa/Kigali"));
        assertTrue(!today.isBefore(before) && !today.isAfter(after));
        assertEquals(ZoneId.of("Africa/Kigali"), BusinessTime.ZONE);
    }
}
