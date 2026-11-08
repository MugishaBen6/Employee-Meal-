package com.emeal.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated caller from the server-side security context.
 *
 * <p>Security contract: every method that determines ownership or attributes
 * a record to a user MUST read from this class, never from a client-supplied
 * value.  {@link #getCurrentUsername()} throws if the security context does
 * not hold a valid, authenticated principal — it must never silently fall back
 * to a default username, because that would allow unauthenticated or
 * misconfigured callers to create or modify records under a real user's name.
 *
 * <p>All callers of {@link #getCurrentUsername()} are behind Spring Security
 * {@code @PreAuthorize} annotations, so in normal operation the security
 * context is always populated before these methods are reached.  If a call
 * somehow arrives without an authenticated principal (e.g. a misconfigured
 * async thread) the {@link IllegalStateException} will surface immediately
 * rather than silently attributing data to a ghost "admin" account.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Returns the username of the currently authenticated principal.
     *
     * @throws IllegalStateException if no authenticated principal is present
     *                               in the security context.
     */
    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        // AnonymousAuthenticationToken reports isAuthenticated() == true, so it
        // must be rejected explicitly.
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException(
                    "getCurrentUsername() called without an authenticated principal in the security context. " +
                    "Ensure the caller is behind a @PreAuthorize guard or equivalent Spring Security filter.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUsername();
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            throw new IllegalStateException(
                    "Authenticated principal has no valid username (got: '" + name + "'). " +
                    "This indicates a misconfigured security context.");
        }
        return name;
    }

    public static String getCurrentRole() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return "";
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
    }

    public static boolean hasRole(String role) {
        return role != null && role.equals(getCurrentRole());
    }

    public static boolean hasAnyRole(String... roles) {
        String current = getCurrentRole();
        for (String r : roles) {
            if (r != null && r.equals(current)) return true;
        }
        return false;
    }

    public static boolean isUserRole() {
        return hasRole("USER");
    }
}
