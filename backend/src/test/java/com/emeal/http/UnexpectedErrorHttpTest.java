package com.emeal.http;

import com.emeal.entity.Role;
import com.emeal.entity.UserStatus;
import com.emeal.exception.MisconfigurationException;
import com.emeal.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unexpected server-side failures must reach the client as a generic message only.
 * DashboardService is replaced by a mock that throws exceptions full of internals.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class UnexpectedErrorHttpTest extends HttpTestSupport {

    @MockBean
    private DashboardService dashboardService;

    @Test
    void unexpectedException_returnsGeneric500() throws Exception {
        createUser("admin1", Role.ADMIN, UserStatus.ACTIVE);
        String token = login("admin1");
        when(dashboardService.getDashboardStatistics()).thenThrow(new IllegalStateException(
                "org.hibernate.exception.SQLGrammarException: JDBC failed: SELECT password FROM users",
                new SQLException("ERROR: relation \"users\" does not exist")));

        HttpResponse<String> res = get("/dashboard/statistics", token);

        assertEquals(500, res.statusCode());
        assertSafeErrorBody(res);
        assertEquals("An unexpected error occurred. Please try again or contact an administrator.",
                json(res).path("message").asText());
    }

    @Test
    void dataIntegrityViolation_returns409WithoutSql() throws Exception {
        createUser("admin1", Role.ADMIN, UserStatus.ACTIVE);
        String token = login("admin1");
        when(dashboardService.getDashboardStatistics()).thenThrow(new DataIntegrityViolationException(
                "could not execute statement; SQL [insert into meal_records ...]; constraint [uk_employee_meal_date]"));

        HttpResponse<String> res = get("/dashboard/statistics", token);

        assertEquals(409, res.statusCode());
        assertSafeErrorBody(res);
    }

    @Test
    void missingMealPriceSetting_failsClearlyWithSafeMessage() throws Exception {
        createUser("admin1", Role.ADMIN, UserStatus.ACTIVE);
        String token = login("admin1");
        when(dashboardService.getDashboardStatistics()).thenThrow(new MisconfigurationException(
                "The standard meal price is not configured. An administrator must set it in Settings."));

        HttpResponse<String> res = get("/dashboard/statistics", token);

        assertEquals(500, res.statusCode());
        assertSafeErrorBody(res);
        assertEquals("The standard meal price is not configured. An administrator must set it in Settings.",
                json(res).path("message").asText());
    }
}
