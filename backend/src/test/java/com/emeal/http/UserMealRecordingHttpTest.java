package com.emeal.http;

import com.emeal.entity.AuditLog;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.entity.Role;
import com.emeal.entity.UserStatus;
import com.emeal.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * USER / EMPLOYEE meal recording over real HTTP with real JWTs, as used by the USER dashboard:
 * record ATE and DID_NOT_EAT, amounts calculated by the server, see and edit only own records,
 * audit trail on every change, and no access to other roles' functions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class UserMealRecordingHttpTest extends HttpTestSupport {

    @Autowired private AuditLogRepository auditLogRepository;

    private String alice;
    private String bob;
    private String hr;
    private Employee e1;
    private Employee e2;
    private Employee e3;

    @BeforeEach
    void seed() throws Exception {
        createUser("alice", Role.USER, UserStatus.ACTIVE);
        createUser("bob", Role.USER, UserStatus.ACTIVE);
        createUser("hr1", Role.HR, UserStatus.ACTIVE);
        alice = login("alice");
        bob = login("bob");
        hr = login("hr1");
        e1 = employee("UM001", "Alice Worker");
        e2 = employee("UM002", "Eric Worker");
        e3 = employee("UM003", "Jean Worker");
    }

    @Test
    void user_recordsAte_withServerCalculatedPrice_ignoringClientAmount() throws Exception {
        HttpResponse<String> res = post(alice, e1, "2026-10-01", "ATE", "99999");
        assertEquals(201, res.statusCode(), res.body());
        JsonNode rec = json(res).path("data");
        assertEquals("alice", rec.path("recordedBy").asText(), "owner is the logged-in USER");
        assertEquals("ATE", rec.path("mealStatus").asText());
        assertEquals(0, new BigDecimal("600.00").compareTo(rec.path("amount").decimalValue()),
                "USER cannot choose the amount; ATE uses the configured price");
    }

    @Test
    void user_recordsDidNotEat_withZeroAmount() throws Exception {
        HttpResponse<String> res = post(alice, e2, "2026-10-01", "DID_NOT_EAT", "500");
        assertEquals(201, res.statusCode(), res.body());
        assertEquals("DID_NOT_EAT", json(res).path("data").path("mealStatus").asText());
        assertEquals(0, BigDecimal.ZERO.compareTo(json(res).path("data").path("amount").decimalValue()));
    }

    @Test
    void user_seesOnlyOwnRecords_andCannotTouchOthers() throws Exception {
        long mine = id(post(alice, e1, "2026-10-01", "ATE", null));
        long bobs = id(post(bob, e2, "2026-10-01", "ATE", null));

        JsonNode list = json(get("/meals?startDate=2026-10-01&endDate=2026-10-01&size=50", alice)).path("data").path("content");
        assertEquals(1, list.size());
        assertEquals(mine, list.get(0).path("id").asLong());

        assertEquals(403, get("/meals/" + bobs, alice).statusCode());
        assertEquals(403, send("PUT", "/meals/" + bobs, alice, "{\"mealStatus\":\"DID_NOT_EAT\",\"amount\":0}").statusCode());
        assertEquals(403, send("DELETE", "/meals/" + mine, alice, null).statusCode(), "USER cannot delete, even own records");
        MealRecord stillBobs = mealRecordRepository.findById(bobs).orElseThrow();
        assertEquals(MealStatus.ATE, stillBobs.getMealStatus());
        assertEquals("bob", stillBobs.getRecordedBy());
    }

    @Test
    void duplicateForSameEmployeeAndDay_isRejected_withoutRevealingTheOtherAccount() throws Exception {
        post(bob, e3, "2026-10-01", "ATE", null);
        HttpResponse<String> dup = post(alice, e3, "2026-10-01", "DID_NOT_EAT", null);
        assertEquals(400, dup.statusCode());
        assertFalse(dup.body().contains("bob"), "must not reveal who recorded it: " + dup.body());

        JsonNode check = json(get("/meals/quick-check?query=" + e3.getId() + "&date=2026-10-01", alice)).path("data");
        assertTrue(check.path("alreadyRecordedToday").asBoolean());
        assertTrue(check.path("todayRecord").isNull(), "another account's record stays hidden from USER");
    }

    @Test
    void user_editsOwnRecords_amountFollowsPricingRules_andEveryChangeIsAudited() throws Exception {
        long rec = id(post(alice, e1, "2026-10-01", "ATE", null));

        // ATE -> DID_NOT_EAT: 0
        JsonNode a = json(send("PUT", "/meals/" + rec, alice, "{\"mealStatus\":\"DID_NOT_EAT\",\"amount\":700}")).path("data");
        assertEquals("DID_NOT_EAT", a.path("mealStatus").asText());
        assertEquals(0, BigDecimal.ZERO.compareTo(a.path("amount").decimalValue()));
        // DID_NOT_EAT -> ATE: configured price, client amount ignored
        JsonNode b = json(send("PUT", "/meals/" + rec, alice, "{\"mealStatus\":\"ATE\",\"amount\":5000}")).path("data");
        assertEquals(0, new BigDecimal("600.00").compareTo(b.path("amount").decimalValue()));
        assertEquals("alice", b.path("recordedBy").asText(), "ownership never changes");

        // An existing ATE record keeps its stored (historical) amount; history is not repriced.
        MealRecord old = mealRecordRepository.save(MealRecord.builder().employee(e2)
                .mealDate(LocalDate.of(2026, 8, 20)).mealStatus(MealStatus.ATE)
                .amount(new BigDecimal("1500.00")).recordedBy("alice").build());
        JsonNode c = json(send("PUT", "/meals/" + old.getId(), alice, "{\"mealStatus\":\"ATE\",\"amount\":9999}")).path("data");
        assertEquals(0, new BigDecimal("1500.00").compareTo(c.path("amount").decimalValue()));

        List<AuditLog> trail = auditLogRepository.findAll().stream()
                .filter(l -> String.valueOf(rec).equals(l.getEntityId())).toList();
        assertTrue(trail.stream().anyMatch(l -> "RECORD_MEAL".equals(l.getAction()) && "alice".equals(l.getUsername())));
        List<AuditLog> updates = trail.stream().filter(l -> "UPDATE_MEAL_RECORD".equals(l.getAction())).toList();
        assertEquals(2, updates.size());
        for (AuditLog l : updates) {
            assertEquals("alice", l.getUsername(), "who made the change");
            assertEquals("USER", l.getUserRole());
            assertNotNull(l.getTimestamp(), "when it happened");
            assertTrue(l.getDescription().contains("PREVIOUS:") && l.getDescription().contains("NEW:"));
        }
    }

    @Test
    void user_hasNoAdminHrAccountantOrDirectorPermissions() throws Exception {
        assertEquals(200, get("/employees/quick-search?query=Worker", alice).statusCode(), "needed to pick an employee");
        assertEquals(200, get("/dashboard/my-stats", alice).statusCode());

        assertEquals(403, get("/employees?size=5", alice).statusCode());
        assertEquals(403, send("POST", "/employees", alice,
                "{\"employeeCode\":\"X1\",\"firstName\":\"X\",\"lastName\":\"Y\",\"department\":\"D\",\"position\":\"P\",\"phone\":\"+250788000999\"}").statusCode());
        assertEquals(403, get("/reports/daily?startDate=2026-10-01&endDate=2026-10-01", alice).statusCode());
        assertEquals(403, get("/dashboard/statistics", alice).statusCode());
        assertEquals(403, get("/audit-logs", alice).statusCode());
        assertEquals(403, get("/users", alice).statusCode());
        assertEquals(403, get("/settings", alice).statusCode());
        assertEquals(403, get("/expenses/summary", alice).statusCode());
    }

    @Test
    void otherRoles_keepTheirExistingAmountBehaviour() throws Exception {
        JsonNode rec = json(post(hr, e1, "2026-10-02", "ATE", "700")).path("data");
        assertEquals(0, new BigDecimal("700").compareTo(rec.path("amount").decimalValue()),
                "HR may still enter an explicit positive amount");
        assertEquals("hr1", rec.path("recordedBy").asText());
    }

    // ---------- helpers ----------

    private HttpResponse<String> post(String token, Employee emp, String date, String status, String amount) throws Exception {
        String body = "{\"employeeId\":" + emp.getId() + ",\"mealDate\":\"" + date + "\",\"mealStatus\":\"" + status + "\""
                + (amount != null ? ",\"amount\":" + amount : "") + "}";
        return send("POST", "/meals", token, body);
    }

    private long id(HttpResponse<String> res) throws Exception {
        assertEquals(201, res.statusCode(), res.body());
        return json(res).path("data").path("id").asLong();
    }

    private Employee employee(String code, String name) {
        String[] parts = name.split(" ");
        return employeeRepository.save(Employee.builder()
                .employeeCode(code).firstName(parts[0]).lastName(parts[1])
                .department("Production").position("Worker").phone("+25078866" + code.substring(2) + "0")
                .status(EmployeeStatus.ACTIVE).build());
    }
}
