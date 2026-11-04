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
        // USER may only add a new person together with their meal record; a plain employee
        // creation (HR-style, no meal status) is refused.
        assertEquals(400, send("POST", "/employees", alice,
                "{\"employeeCode\":\"X1\",\"firstName\":\"X\",\"lastName\":\"Y\",\"department\":\"D\",\"position\":\"P\",\"phone\":\"+250788000999\"}").statusCode());
        assertFalse(employeeRepository.existsByEmployeeCode("X1"));
        assertEquals(403, get("/reports/daily?startDate=2026-10-01&endDate=2026-10-01", alice).statusCode());
        assertEquals(403, get("/dashboard/statistics", alice).statusCode());
        assertEquals(403, get("/audit-logs", alice).statusCode());
        assertEquals(403, get("/users", alice).statusCode());
        assertEquals(403, get("/settings", alice).statusCode());
        assertEquals(403, get("/expenses/summary", alice).statusCode());
    }

    // ---------- USER adds a person who is not yet on the list ----------

    @Test
    void user_addsNewEmployeeWhoAte_withMealRecordedAndOwnedByUser() throws Exception {
        HttpResponse<String> res = newEmployee(alice, "Grace Mukamana", "+250788123456", "Cleaner",
                "ATE", "2026-10-03", ",\"amount\":99999,\"employeeCode\":\"HACK1\"");
        assertEquals(201, res.statusCode(), res.body());
        JsonNode emp = json(res).path("data");
        assertEquals("Grace Mukamana", emp.path("fullName").asText());
        assertEquals("Cleaner", emp.path("position").asText());
        assertEquals("ACTIVE", emp.path("status").asText());
        assertNotEquals("HACK1", emp.path("employeeCode").asText(), "USER cannot choose the employee ID");

        // The new person is immediately searchable, and the meal is recorded by alice at the standard price.
        assertEquals(200, get("/employees/quick-search?query=Mukamana", alice).statusCode());
        JsonNode mine = json(get("/meals?startDate=2026-10-03&endDate=2026-10-03&size=50", alice)).path("data").path("content");
        assertEquals(1, mine.size());
        assertEquals("alice", mine.get(0).path("recordedBy").asText());
        assertEquals("ATE", mine.get(0).path("mealStatus").asText());
        assertEquals(0, new BigDecimal("600.00").compareTo(mine.get(0).path("amount").decimalValue()));

        // Audited with the real actor (not "SYSTEM").
        String empId = emp.path("id").asText();
        AuditLog created = auditLogRepository.findAll().stream()
                .filter(l -> "CREATE_EMPLOYEE".equals(l.getAction()) && empId.equals(l.getEntityId()))
                .findFirst().orElseThrow();
        assertEquals("alice", created.getUsername());
        assertEquals("USER", created.getUserRole());
        assertNotNull(created.getTimestamp());
    }

    @Test
    void user_addsNewEmployeeWhoDidNotEat_withZeroAmount() throws Exception {
        HttpResponse<String> res = newEmployee(alice, "Paul Habimana", "+250788654321", "Guard",
                "DID_NOT_EAT", "2026-10-03", "");
        assertEquals(201, res.statusCode(), res.body());
        JsonNode mine = json(get("/meals?startDate=2026-10-03&endDate=2026-10-03&size=50", alice)).path("data").path("content");
        assertEquals("DID_NOT_EAT", mine.get(0).path("mealStatus").asText());
        assertEquals(0, BigDecimal.ZERO.compareTo(mine.get(0).path("amount").decimalValue()));
    }

    @Test
    void user_newEmployee_rules_areEnforcedOnTheServer() throws Exception {
        long before = employeeRepository.count();
        // A meal status is required (a USER only adds people while recording their meal).
        assertEquals(400, newEmployee(alice, "No Meal", "+250788000001", "Worker", null, "2026-10-03", "").statusCode());
        // Phone number already belongs to an existing employee -> no duplicate person.
        HttpResponse<String> dup = newEmployee(alice, "Copy Person", e1.getPhone(), "Worker", "ATE", "2026-10-03", "");
        assertEquals(400, dup.statusCode());
        assertTrue(json(dup).path("message").asText().contains("already exists"));
        // No future meal dates.
        assertEquals(400, newEmployee(alice, "Future Person", "+250788000002", "Worker", "ATE",
                LocalDate.now().plusDays(3).toString(), "").statusCode());
        // Name, position and phone are required.
        assertEquals(400, newEmployee(alice, "", "+250788000003", "Worker", "ATE", "2026-10-03", "").statusCode());
        assertEquals(400, newEmployee(alice, "No Phone", "", "Worker", "ATE", "2026-10-03", "").statusCode());
        assertEquals(400, newEmployee(alice, "No Position", "+250788000004", "", "ATE", "2026-10-03", "").statusCode());
        assertEquals(before, employeeRepository.count(), "nothing was created by rejected requests");
    }

    @Test
    void user_stillCannotEditDeactivateOrListEmployees() throws Exception {
        long id = json(newEmployee(alice, "Lucy Uwera", "+250788777666", "Cook", "ATE", "2026-10-03", ""))
                .path("data").path("id").asLong();
        assertEquals(403, send("PUT", "/employees/" + id, alice,
                "{\"employeeName\":\"Changed\",\"phone\":\"+250788777666\",\"position\":\"Cook\"}").statusCode());
        assertEquals(403, send("DELETE", "/employees/" + id, alice, null).statusCode());
        assertEquals(403, send("POST", "/employees/bulk-delete", alice, "[" + id + "]").statusCode());
        assertEquals(403, get("/employees?size=5", alice).statusCode());
    }

    @Test
    void hr_createEmployee_isUnchanged() throws Exception {
        HttpResponse<String> res = send("POST", "/employees", hr,
                "{\"employeeCode\":\"HR777\",\"employeeName\":\"Hr Created\",\"phone\":\"+250788555444\",\"position\":\"Clerk\"}");
        assertEquals(201, res.statusCode(), res.body());
        assertEquals("HR777", json(res).path("data").path("employeeCode").asText(), "HR may still set the ID");
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

    private HttpResponse<String> newEmployee(String token, String name, String phone, String position,
                                             String status, String date, String extra) throws Exception {
        String body = "{\"employeeName\":\"" + name + "\",\"phone\":\"" + phone + "\",\"position\":\"" + position + "\""
                + (status != null ? ",\"mealStatus\":\"" + status + "\"" : "")
                + ",\"mealDate\":\"" + date + "\"" + extra + "}";
        return send("POST", "/employees", token, body);
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
