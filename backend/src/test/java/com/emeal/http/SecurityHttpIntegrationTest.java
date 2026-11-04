package com.emeal.http;

import com.emeal.config.JwtUtils;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.Role;
import com.emeal.entity.User;
import com.emeal.entity.UserStatus;
import com.emeal.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real HTTP + real JWT checks for authentication (401), authorization (403),
 * inactive accounts holding an old token, and client-input errors (400).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class SecurityHttpIntegrationTest extends HttpTestSupport {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // ---------- 401: no / invalid / expired authentication ----------

    @Test
    void missingToken_returns401WithSafeJson() throws Exception {
        HttpResponse<String> res = get("/meals", null);
        assertEquals(401, res.statusCode());
        assertTrue(res.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertSafeErrorBody(res);
        assertEquals("Authentication required. Please log in again.", json(res).path("message").asText());

        HttpResponse<String> post = send("POST", "/meals", null, "{\"employeeId\":1}");
        assertEquals(401, post.statusCode());
        // URL-level rule (/settings is ADMIN-only): anonymous is 401, not 403.
        assertEquals(401, get("/settings", null).statusCode());
    }

    @Test
    void malformedTamperedAndExpiredTokens_return401() throws Exception {
        createUser("alice", Role.USER, UserStatus.ACTIVE);
        String token = login("alice");
        assertEquals(200, get("/meals", token).statusCode());

        assertEquals(401, get("/meals", "not.a.jwt").statusCode());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        assertEquals(401, get("/meals", tampered).statusCode());

        // Correctly signed with the real secret, but already expired.
        JwtUtils expiredIssuer = new JwtUtils();
        ReflectionTestUtils.setField(expiredIssuer, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(expiredIssuer, "jwtExpirationMs", -60_000);
        User alice = userRepository.findByUsername("alice").orElseThrow();
        UserPrincipal principal = UserPrincipal.build(alice);
        String expired = expiredIssuer.generateJwtToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        HttpResponse<String> res = get("/meals", expired);
        assertEquals(401, res.statusCode());
        assertSafeErrorBody(res);
    }

    // ---------- inactive / non-active accounts holding an old token ----------

    @Test
    void inactiveUser_cannotUseOldToken_untilReactivated() throws Exception {
        User alice = createUser("alice", Role.USER, UserStatus.ACTIVE);
        String oldToken = login("alice");
        assertEquals(200, get("/meals", oldToken).statusCode());

        alice.setStatus(UserStatus.INACTIVE);
        userRepository.save(alice);
        HttpResponse<String> res = get("/meals", oldToken);
        assertEquals(401, res.statusCode(), "deactivated account must not be able to use its old JWT");
        assertSafeErrorBody(res);
        assertEquals(401, send("POST", "/meals", oldToken, "{\"employeeId\":1}").statusCode());
        assertEquals(401, get("/dashboard/my-stats", oldToken).statusCode());

        alice.setStatus(UserStatus.REJECTED);
        userRepository.save(alice);
        assertEquals(401, get("/meals", oldToken).statusCode());

        alice.setStatus(UserStatus.PENDING_APPROVAL);
        userRepository.save(alice);
        assertEquals(401, get("/meals", oldToken).statusCode());

        alice.setStatus(UserStatus.ACTIVE);
        userRepository.save(alice);
        assertEquals(200, get("/meals", oldToken).statusCode(), "re-activated account works again");
    }

    @Test
    void deactivatedAdmin_loses_adminAccess_immediately() throws Exception {
        User admin = createUser("admin1", Role.ADMIN, UserStatus.ACTIVE);
        String token = login("admin1");
        assertEquals(200, get("/users", token).statusCode());

        admin.setStatus(UserStatus.INACTIVE);
        userRepository.save(admin);
        assertEquals(401, get("/users", token).statusCode());
    }

    @Test
    void deletedUsersToken_returns401() throws Exception {
        User bob = createUser("bob", Role.USER, UserStatus.ACTIVE);
        String token = login("bob");
        userRepository.delete(bob);
        assertEquals(401, get("/meals", token).statusCode());
    }

    // ---------- 403: authenticated but not permitted ----------

    @Test
    void authenticatedWithoutPermission_returns403WithSafeJson() throws Exception {
        createUser("alice", Role.USER, UserStatus.ACTIVE);
        createUser("hr1", Role.HR, UserStatus.ACTIVE);
        String alice = login("alice");
        String hr = login("hr1");

        // URL-level rule -> accessDeniedHandler
        HttpResponse<String> users = get("/users", alice);
        assertEquals(403, users.statusCode());
        assertSafeErrorBody(users);
        assertEquals("Access denied. You do not have permission to perform this action.",
                json(users).path("message").asText());
        assertEquals(403, get("/settings", hr).statusCode());

        // Method-level @PreAuthorize -> GlobalExceptionHandler
        HttpResponse<String> audit = get("/audit-logs", alice);
        assertEquals(403, audit.statusCode());
        assertSafeErrorBody(audit);
        assertEquals(403, get("/dashboard/statistics", alice).statusCode());
        assertEquals(403, get("/dashboard/my-stats", hr).statusCode());
        assertEquals(403, send("DELETE", "/meals/1", alice, null).statusCode());
        assertEquals(403, send("DELETE", "/meals/1", hr, null).statusCode());

        // Legitimate access is unchanged.
        assertEquals(200, get("/meals", alice).statusCode());
        assertEquals(200, get("/dashboard/my-stats", alice).statusCode());
        assertEquals(200, get("/dashboard/statistics", hr).statusCode());
    }

    // ---------- 400: invalid client input never leaks internals ----------

    @Test
    void invalidClientInput_returns400WithoutInternals() throws Exception {
        createUser("hr1", Role.HR, UserStatus.ACTIVE);
        String hr = login("hr1");
        Employee emp = employeeRepository.save(Employee.builder()
                .employeeCode("HTTP001").firstName("Http").lastName("Test")
                .department("Production").position("Worker").phone("+250788000111")
                .status(EmployeeStatus.ACTIVE).build());

        HttpResponse<String>[] cases = new HttpResponse[]{
                get("/meals/abc", hr),                                   // invalid id type
                get("/meals?page=x", hr),                                // invalid pagination
                get("/meals?size=lots", hr),
                get("/meals/quick-check", hr),                           // missing required param
                get("/meals/quick-check?query=HTTP001&date=31-12-2026", hr), // bad date
                send("POST", "/meals", hr, "{bad json"),                // malformed JSON
                send("POST", "/meals", hr, "{\"employeeId\":" + emp.getId() + ",\"mealStatus\":\"HACK\"}"), // invalid enum
                send("POST", "/meals", hr, "{\"employeeId\":\"x\"}"),  // wrong JSON type
                send("POST", "/meals", hr, "{\"employeeId\":" + emp.getId() + ",\"mealDate\":\"2026-13-45\"}"),
                send("PUT", "/meals/abc", hr, "{\"mealStatus\":\"ATE\"}"),
        };
        for (HttpResponse<String> res : cases) {
            assertEquals(400, res.statusCode(), res.request().method() + " " + res.uri() + " -> " + res.body());
            assertSafeErrorBody(res);
        }
        assertEquals("Invalid value for parameter 'id'.", json(cases[0]).path("message").asText());
        assertEquals("Malformed request body or invalid field value.", json(cases[5]).path("message").asText());

        // Other framework errors are mapped to their proper status, also without internals.
        HttpResponse<String> unknown = get("/no-such-endpoint", hr);
        assertEquals(404, unknown.statusCode());
        assertSafeErrorBody(unknown);
        HttpResponse<String> method = send("PATCH", "/meals", hr, "{}");
        assertEquals(405, method.statusCode());
        assertSafeErrorBody(method);

        // A valid request still works.
        assertEquals(201, send("POST", "/meals", hr,
                "{\"employeeId\":" + emp.getId() + ",\"mealDate\":\"2026-10-01\",\"mealStatus\":\"ATE\"}").statusCode());
    }
}
