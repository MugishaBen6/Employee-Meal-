package com.emeal.http;

import com.emeal.entity.Role;
import com.emeal.entity.User;
import com.emeal.entity.UserStatus;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import com.emeal.repository.PasswordResetTokenRepository;
import com.emeal.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Base class for tests that talk to the application over real HTTP on a random local
 * port, with JWTs obtained from the real /api/auth/login endpoint.
 * Subclasses use @SpringBootTest(webEnvironment = RANDOM_PORT). Data is committed,
 * so every table touched is emptied before and after each test.
 */
public abstract class HttpTestSupport {

    protected static final String PASSWORD = "Pass1234!";

    @LocalServerPort
    protected int port;

    @Autowired protected UserRepository userRepository;
    @Autowired protected EmployeeRepository employeeRepository;
    @Autowired protected MealRecordRepository mealRecordRepository;
    @Autowired protected PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected ObjectMapper objectMapper;

    protected final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @BeforeEach
    void cleanBefore() {
        cleanTables();
    }

    @AfterEach
    void cleanAfter() {
        cleanTables();
    }

    @Autowired protected org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    protected void cleanTables() {
        jdbcTemplate.update("DELETE FROM trash_items");
        mealRecordRepository.deleteAll();
        employeeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String username, Role role, UserStatus status) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@test.local")
                .password(passwordEncoder.encode(PASSWORD))
                .firstName("Test")
                .lastName(username)
                .role(role)
                .status(status)
                .build());
    }

    /** Logs in through the real endpoint and returns the issued JWT. */
    protected String login(String username) throws Exception {
        HttpResponse<String> res = send("POST", "/auth/login", null,
                "{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, res.statusCode(), "login failed: " + res.body());
        return json(res).path("data").path("token").asText();
    }

    protected HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> get(String path, String token) throws Exception {
        return send("GET", path, token, null);
    }

    protected JsonNode json(HttpResponse<String> res) throws Exception {
        return objectMapper.readTree(res.body());
    }

    /** The body must be the standard error JSON and must not leak implementation details. */
    protected void assertSafeErrorBody(HttpResponse<String> res) throws Exception {
        JsonNode body = json(res);
        assertFalse(body.path("success").asBoolean(true), "success must be false: " + res.body());
        String text = res.body();
        for (String leak : new String[]{"java.", "javax.", "com.emeal", "org.springframework", "Exception",
                "SQL", "select ", "SELECT ", "Hibernate", "JDBC", "\tat ", "trace"}) {
            assertFalse(text.contains(leak), "response leaks '" + leak + "': " + text);
        }
    }
}
