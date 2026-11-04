package com.emeal.http;

import com.emeal.entity.Role;
import com.emeal.entity.User;
import com.emeal.entity.UserStatus;
import com.emeal.security.AdminBootstrapLock;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrent first-administrator creation over real HTTP.
 *
 * <p>Before the fix, 10 simultaneous /auth/setup-admin requests against an empty
 * PostgreSQL database created 10 ACTIVE administrators. The advisory lock exists only
 * on PostgreSQL, so this test runs only when the suite is pointed at PostgreSQL
 * (it is reported as skipped on the H2 profile).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class AdminSetupConcurrencyTest extends HttpTestSupport {

    private static final int REQUESTS = 12;

    @Autowired
    private AdminBootstrapLock adminBootstrapLock;

    @Test
    void simultaneousAdminCreation_yieldsExactlyOneActiveAdmin() throws Exception {
        Assumptions.assumeTrue(adminBootstrapLock.isActive(),
                "Requires PostgreSQL (pg_advisory_xact_lock); run the suite against a disposable PostgreSQL database");
        assertEquals(0, userRepository.count(), "test needs an empty users table");

        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch ready = new CountDownLatch(REQUESTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> results = new ArrayList<>();
        try {
            for (int i = 0; i < REQUESTS; i++) {
                final int n = i;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    if (n % 2 == 0) {
                        return send("POST", "/auth/setup-admin", null,
                                "{\"firstName\":\"Race\",\"lastName\":\"S" + n + "\",\"username\":\"race" + n
                                        + "\",\"email\":\"race" + n + "@test.local\",\"password\":\"Secret123!\"}");
                    }
                    return send("POST", "/auth/register", null,
                            "{\"firstName\":\"Race\",\"lastName\":\"R" + n + "\",\"username\":\"race" + n
                                    + "\",\"email\":\"race" + n + "@test.local\",\"password\":\"Secret123!\","
                                    + "\"confirmPassword\":\"Secret123!\",\"role\":\"ADMIN\"}");
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            go.countDown(); // release all requests at once
            for (Future<HttpResponse<String>> f : results) {
                HttpResponse<String> res = f.get(60, TimeUnit.SECONDS);
                assertEquals(201, res.statusCode(), res.body());
            }
        } finally {
            pool.shutdownNow();
        }

        List<User> admins = userRepository.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).toList();
        assertEquals(REQUESTS, admins.size());
        assertEquals(1, admins.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count(),
                "exactly one ACTIVE administrator");
        assertEquals(REQUESTS - 1, admins.stream().filter(u -> u.getStatus() == UserStatus.PENDING_APPROVAL).count(),
                "every other administrator awaits approval");
    }
}
