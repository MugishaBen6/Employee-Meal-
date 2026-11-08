package com.emeal.http;

import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.Role;
import com.emeal.entity.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Trash / Recycle Bin, bulk employee deletion, historical meal editing and USER own-record editing,
 * over real HTTP with real logins. Report totals are checked after every change.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class TrashAndEditingHttpTest extends HttpTestSupport {

    private String admin, hr, accountant, alice, bob;

    @BeforeEach
    void seed() throws Exception {
        jdbcTemplate.update("DELETE FROM audit_logs");
        createUser("admin1", Role.ADMIN, UserStatus.ACTIVE);
        createUser("hr1", Role.HR, UserStatus.ACTIVE);
        createUser("acc1", Role.ACCOUNTANT, UserStatus.ACTIVE);
        createUser("alice", Role.USER, UserStatus.ACTIVE);
        createUser("bob", Role.USER, UserStatus.ACTIVE);
        admin = login("admin1");
        hr = login("hr1");
        accountant = login("acc1");
        alice = login("alice");
        bob = login("bob");
    }

    // ---------------------------------------------------------------- Trash: employees

    @Test
    void deletedEmployeeGoesToTrash_isExcludedEverywhere_andRestoresExactly() throws Exception {
        Employee e = employee("T001", "Grace Uwase");
        Employee other = employee("T002", "Eric Habimana");
        long m1 = meal(hr, e, "2026-10-05", "ATE", "1500");
        long m2 = meal(hr, e, "2026-10-06", "DID_NOT_EAT", null);
        meal(hr, other, "2026-10-05", "ATE", "600");
        assertReport(2100, 2);

        Map<String, Object> before = jdbcTemplate.queryForMap("SELECT * FROM meal_records WHERE id = ?", m1);

        HttpResponse<String> del = send("POST", "/employees/bulk-delete", hr, "[" + e.getId() + "]");
        assertEquals(200, del.statusCode(), del.body());
        assertEquals(1, json(del).path("data").asInt());

        // Gone from active data: employee list, meal records, reports.
        assertFalse(employeeRepository.existsById(e.getId()));
        assertEquals(0, count("SELECT COUNT(*) FROM meal_records WHERE employee_id = " + e.getId()));
        assertReport(600, 1);
        JsonNode list = json(get("/employees/attendance?date=2026-10-05&size=50", admin)).path("data").path("employees").path("content");
        assertEquals(1, list.size());
        // After all meal records of a day are deleted, that day's table is empty.
        long otherMeal = jdbcTemplate.queryForObject("SELECT id FROM meal_records WHERE meal_date = '2026-10-05'", Long.class);
        assertEquals(200, send("DELETE", "/meals/" + otherMeal, admin, null).statusCode());
        assertEquals(0, json(get("/employees/attendance?date=2026-10-05&size=50", admin)).path("data").path("employees").path("totalElements").asInt());
        long otherTrash = trash("MEAL_RECORD").get(0).path("id").asLong();
        assertEquals(1, json(send("POST", "/trash/restore", admin, "[" + otherTrash + "]")).path("data").path("restored").asInt());

        // Visible in Trash with who/when and its meals.
        JsonNode items = trash("EMPLOYEE");
        assertEquals(1, items.size());
        JsonNode item = items.get(0);
        assertEquals("Grace Uwase", item.path("employeeName").asText());
        assertEquals("T001", item.path("employeeCode").asText());
        assertEquals("hr1", item.path("deletedBy").asText());
        assertEquals(2, item.path("mealCount").asInt());
        assertEquals(0, new BigDecimal("1500").compareTo(item.path("mealAmount").decimalValue()));
        assertFalse(item.path("deletedAt").isNull());

        // Restore -> identical rows (same ids, dates, amounts, status, owner, timestamps).
        HttpResponse<String> res = send("POST", "/trash/restore", admin, "[" + item.path("id").asLong() + "]");
        assertEquals(200, res.statusCode(), res.body());
        assertEquals(1, json(res).path("data").path("restored").asInt());
        assertTrue(employeeRepository.existsById(e.getId()));
        Map<String, Object> after = jdbcTemplate.queryForMap("SELECT * FROM meal_records WHERE id = ?", m1);
        assertEquals(before, after, "restored meal record must equal the original row");
        assertEquals(1, count("SELECT COUNT(*) FROM meal_records WHERE id = " + m2 + " AND meal_status = 'DID_NOT_EAT'"));
        assertEquals("T001", employeeRepository.findById(e.getId()).orElseThrow().getEmployeeCode());
        assertEquals(0, trash(null).size());
        assertReport(2100, 2);

        // Restoring the same item again cannot duplicate anything.
        HttpResponse<String> again = send("POST", "/trash/restore", admin, "[" + item.path("id").asLong() + "]");
        assertEquals(0, json(again).path("data").path("restored").asInt());
        assertReport(2100, 2);

        assertAudit("TRASH_EMPLOYEE", "hr1", "Grace Uwase");
        assertAudit("RESTORE_EMPLOYEE", "admin1", "Grace Uwase");
    }

    @Test
    void selectAllAcrossPages_bulkDeletesEveryMatchingEmployee_andOnlyThose() throws Exception {
        for (int i = 1; i <= 25; i++) employee(String.format("B%03d", i), "Bulk Person" + i);
        Employee keep = employee("K001", "Keep Me");
        keep.setDepartment("Office");
        employeeRepository.save(keep);

        // The page shows 10 at a time; "select all" asks the server for every matching id.
        // Nobody has a meal that day: the default view lists no one; "All staff" lists everyone.
        assertEquals(0, json(get("/employees/attendance/ids?date=2026-10-05&department=Production", hr)).path("data").size());
        assertEquals(0, json(get("/employees/attendance?date=2026-10-05&size=10", hr)).path("data").path("employees").path("totalElements").asInt());
        JsonNode ids = json(get("/employees/attendance/ids?date=2026-10-05&department=Production&mealStatus=ALL_STAFF", hr)).path("data");
        assertEquals(25, ids.size());
        JsonNode staffRow = json(get("/employees/attendance?date=2026-10-05&mealStatus=ALL_STAFF&size=1", hr)).path("data").path("employees").path("content").get(0);
        assertEquals("NOT_RECORDED", staffRow.path("mealStatus").asText());
        assertEquals(0, BigDecimal.ZERO.compareTo(staffRow.path("amount").decimalValue()));
        assertEquals(403, get("/employees/attendance/ids", accountant).statusCode());

        HttpResponse<String> del = send("POST", "/employees/bulk-delete", admin, ids.toString());
        assertEquals(200, del.statusCode(), del.body());
        assertEquals(25, json(del).path("data").asInt());
        assertEquals(1, employeeRepository.count());
        assertTrue(employeeRepository.existsById(keep.getId()));
        assertEquals(25, json(get("/trash/ids?type=EMPLOYEE", admin)).path("data").size());

        // Bulk restore of all 25.
        HttpResponse<String> res = send("POST", "/trash/restore", admin, json(get("/trash/ids", admin)).path("data").toString());
        assertEquals(25, json(res).path("data").path("restored").asInt());
        assertEquals(26, employeeRepository.count());
    }

    @Test
    void restoreRefusesDuplicates_andEmployeeFirstRule() throws Exception {
        Employee e = employee("R001", "Diane Ingabire");
        long m = meal(hr, e, "2026-10-07", "ATE", "600");

        // Meal record deleted, then a new one recorded for the same day: restore is refused.
        assertEquals(200, send("DELETE", "/meals/" + m, accountant, null).statusCode());
        assertEquals(0, count("SELECT COUNT(*) FROM meal_records"));
        JsonNode mealItem = trash("MEAL_RECORD").get(0);
        assertEquals("2026-10-07", mealItem.path("mealDate").asText());
        assertEquals("acc1", mealItem.path("deletedBy").asText());
        long replacement = meal(hr, e, "2026-10-07", "DID_NOT_EAT", null);
        JsonNode r = json(send("POST", "/trash/restore", admin, "[" + mealItem.path("id").asLong() + "]")).path("data");
        assertEquals(0, r.path("restored").asInt());
        assertTrue(r.path("results").get(0).path("message").asText().contains("already exists"));
        assertEquals(1, count("SELECT COUNT(*) FROM meal_records"));

        // Remove the replacement; now the original restores into its day.
        assertEquals(200, send("DELETE", "/meals/" + replacement, admin, null).statusCode());
        JsonNode items = trash("MEAL_RECORD");
        long original = 0;
        for (JsonNode it : items) if (it.path("originalId").asLong() == m) original = it.path("id").asLong();
        assertEquals(1, json(send("POST", "/trash/restore", admin, "[" + original + "]")).path("data").path("restored").asInt());
        assertReport(600, 1);

        // A meal whose employee is also in Trash: refused alone, restored together with the employee.
        long m2 = meal(hr, e, "2026-10-08", "ATE", "600");
        assertEquals(200, send("DELETE", "/meals/" + m2, admin, null).statusCode());
        assertEquals(200, send("POST", "/employees/bulk-delete", admin, "[" + e.getId() + "]").statusCode());
        long mealTrash = 0, empTrash = 0;
        for (JsonNode it : trash(null)) {
            if (it.path("originalId").asLong() == m2) mealTrash = it.path("id").asLong();
            if ("EMPLOYEE".equals(it.path("itemType").asText())) empTrash = it.path("id").asLong();
        }
        JsonNode alone = json(send("POST", "/trash/restore", admin, "[" + mealTrash + "]")).path("data");
        assertTrue(alone.path("results").get(0).path("message").asText().contains("Restore employee R001"));
        JsonNode both = json(send("POST", "/trash/restore", admin, "[" + mealTrash + "," + empTrash + "]")).path("data");
        assertEquals(2, both.path("restored").asInt(), both.toString());
        assertReport(1200, 2);

        // Employee code reused by someone else while in Trash: restore refused, nothing changed.
        assertEquals(200, send("POST", "/employees/bulk-delete", admin, "[" + e.getId() + "]").statusCode());
        employee("R001", "Other Person");
        JsonNode conflict = json(send("POST", "/trash/restore", admin, json(get("/trash/ids?type=EMPLOYEE", admin)).path("data").toString())).path("data");
        assertEquals(0, conflict.path("restored").asInt());
        assertTrue(conflict.path("results").get(0).path("message").asText().contains("R001 is now used by Other Person"));
    }

    @Test
    void onlyAdminUsesTrash_andPermanentDeleteNeedsConfirmation() throws Exception {
        Employee e = employee("P001", "Patrick Niyonzima");
        meal(hr, e, "2026-10-09", "ATE", "600");
        send("POST", "/employees/bulk-delete", admin, "[" + e.getId() + "]");
        for (String t : new String[]{hr, accountant, alice}) {
            assertEquals(403, get("/trash", t).statusCode());
            assertEquals(403, send("POST", "/trash/restore", t, "[1]").statusCode());
            assertEquals(403, send("POST", "/trash/purge?confirm=DELETE", t, "[1]").statusCode());
        }
        assertEquals(403, send("POST", "/employees/bulk-delete", alice, "[1]").statusCode());
        assertEquals(403, send("DELETE", "/meals/1", alice, null).statusCode());

        String ids = json(get("/trash/ids", admin)).path("data").toString();
        assertEquals(400, send("POST", "/trash/purge?confirm=yes", admin, ids).statusCode());
        assertEquals(1, trash(null).size());
        HttpResponse<String> purge = send("POST", "/trash/purge?confirm=DELETE", admin, ids);
        assertEquals(200, purge.statusCode(), purge.body());
        assertEquals(0, trash(null).size());
        assertFalse(employeeRepository.existsById(e.getId()));
        assertAudit("PERMANENT_DELETE_EMPLOYEE", "admin1", "Patrick Niyonzima");
        // Audit history cannot be changed through the API.
        assertTrue(send("DELETE", "/audit-logs", admin, null).statusCode() >= 400);
    }

    @Test
    void cleanStart_canRemoveAllMealRecordsAndAllEmployees_andRestoreThem() throws Exception {
        Employee a = employee("C001", "Clean One");
        Employee b = employee("C002", "Clean Two");
        meal(hr, a, "2026-10-05", "ATE", "600");
        meal(hr, b, "2026-10-06", "DID_NOT_EAT", null);
        JsonNode sum = json(get("/meals/maintenance/summary", admin)).path("data");
        assertEquals(2, sum.path("totalRecords").asInt());
        assertEquals(2, sum.path("employees").asInt());

        // Stale employee count, or a date range: refused and nothing changes.
        assertEquals(400, send("DELETE", "/meals/maintenance?expectedCount=2&confirm=DELETE&includeEmployees=true&expectedEmployees=3", admin, null).statusCode());
        assertEquals(400, send("DELETE", "/meals/maintenance?startDate=2026-10-01&endDate=2026-10-31&expectedCount=2&confirm=DELETE&includeEmployees=true&expectedEmployees=2", admin, null).statusCode());
        assertEquals(2, employeeRepository.count());
        assertEquals(2, count("SELECT COUNT(*) FROM meal_records"));

        HttpResponse<String> res = send("DELETE", "/meals/maintenance?expectedCount=2&confirm=DELETE&includeEmployees=true&expectedEmployees=2", admin, null);
        assertEquals(200, res.statusCode(), res.body());
        assertEquals(2, json(res).path("data").path("deletedEmployees").asInt());
        assertEquals(0, employeeRepository.count());
        assertEquals(0, count("SELECT COUNT(*) FROM meal_records"));
        assertEquals(0, json(get("/dashboard/statistics", admin)).path("data").path("totalEmployees").asInt());
        assertReport(0, 0);

        HttpResponse<String> back = send("POST", "/trash/restore", admin, json(get("/trash/ids", admin)).path("data").toString());
        assertEquals(2, json(back).path("data").path("restored").asInt());
        assertEquals(2, employeeRepository.count());
        assertReport(600, 1);
    }

    // ---------------------------------------------------------------- Historical editing

    @Test
    void historicalMealEdit_updatesStatusAmountDate_andReportsFollow() throws Exception {
        Employee e = employee("H001", "Jean Bosco");
        long m = meal(hr, e, "2026-10-05", "ATE", "600");
        meal(hr, employee("H002", "Other One"), "2026-10-05", "ATE", "600");
        assertReport(1200, 2);

        // ATE -> DID_NOT_EAT on an old date: amount becomes 0, total drops by 600.
        JsonNode r = json(put(admin, m, "{\"mealStatus\":\"DID_NOT_EAT\",\"amount\":600}"));
        assertEquals("DID_NOT_EAT", r.path("data").path("mealStatus").asText());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.path("data").path("amount").decimalValue()));
        assertEquals("2026-10-05", r.path("data").path("mealDate").asText(), "meal date kept unless changed");
        assertReport(600, 1);

        // DID_NOT_EAT -> ATE without an amount: the standard price (600) is applied.
        r = json(put(hr, m, "{\"mealStatus\":\"ATE\",\"amount\":0}"));
        assertEquals(0, new BigDecimal("600").compareTo(r.path("data").path("amount").decimalValue()));
        assertReport(1200, 2);

        // Move the record to another date: old day loses it, new day gains it.
        r = json(put(accountant, m, "{\"mealStatus\":\"ATE\",\"amount\":600,\"mealDate\":\"2026-10-20\"}"));
        assertEquals("2026-10-20", r.path("data").path("mealDate").asText());
        assertEquals(1, report("2026-10-05", "2026-10-05").path("ateCount").asInt());
        assertEquals(1, report("2026-10-20", "2026-10-20").path("ateCount").asInt());
        assertReport(1200, 2);

        // Cannot move onto a day that already has a record for this employee.
        long other = meal(hr, e, "2026-10-21", "ATE", "600");
        HttpResponse<String> dup = send("PUT", "/meals/" + m, admin, "{\"mealStatus\":\"ATE\",\"amount\":600,\"mealDate\":\"2026-10-21\"}");
        assertEquals(400, dup.statusCode(), dup.body());
        assertTrue(dup.body().contains("already exists"));
        assertEquals(1, count("SELECT COUNT(*) FROM meal_records WHERE id = " + other));

        // ADMIN/HR may correct the employee's name and position; ACCOUNTANT may not.
        assertEquals(403, send("PUT", "/meals/" + m, accountant,
                "{\"mealStatus\":\"ATE\",\"amount\":600,\"employeeName\":\"Jean Bosco N.\"}").statusCode());
        r = json(put(hr, m, "{\"mealStatus\":\"ATE\",\"amount\":600,\"employeeName\":\"Jean Bosco N.\",\"position\":\"Supervisor\"}"));
        Employee renamed = employeeRepository.findById(e.getId()).orElseThrow();
        assertEquals("Jean", renamed.getFirstName());
        assertEquals("Bosco N.", renamed.getLastName());
        assertEquals("Supervisor", renamed.getPosition());

        assertAudit("UPDATE_MEAL_RECORD", "admin1", "status ATE -> DID_NOT_EAT");
        assertAudit("UPDATE_MEAL_RECORD", "acc1", "meal date 2026-10-05 -> 2026-10-20");
        assertAudit("UPDATE_EMPLOYEE", "hr1", "employee name 'Jean Bosco' -> 'Jean Bosco N.'");
    }

    // ---------------------------------------------------------------- USER own records

    @Test
    void userEditsOwnRecord_correctsOwnEmployeeName_butNothingElse() throws Exception {
        // Alice adds a new employee (with a typo) together with a meal, as on the USER dashboard.
        HttpResponse<String> created = send("POST", "/employees", alice,
                "{\"employeeName\":\"John Mugisha\",\"position\":\"Cleaner\",\"mealStatus\":\"ATE\",\"amount\":600,\"mealDate\":\"2026-10-05\"}");
        assertEquals(201, created.statusCode(), created.body());
        long johnId = json(created).path("data").path("id").asLong();
        assertEquals("alice", json(created).path("data").path("createdBy").asText());
        long johnMeal = jdbcTemplate.queryForObject("SELECT id FROM meal_records WHERE employee_id = ?", Long.class, johnId);

        // She corrects the name and the status of her own record; the amount is the server's.
        JsonNode r = json(put(alice, johnMeal, "{\"mealStatus\":\"DID_NOT_EAT\",\"amount\":9999,\"employeeName\":\"John Mugisha N.\",\"position\":\"Senior Cleaner\"}"));
        assertEquals("DID_NOT_EAT", r.path("data").path("mealStatus").asText());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.path("data").path("amount").decimalValue()));
        assertEquals("John Mugisha N.", employeeRepository.findById(johnId).orElseThrow().getFullName());
        r = json(put(alice, johnMeal, "{\"mealStatus\":\"ATE\",\"amount\":9999}"));
        assertEquals(0, new BigDecimal("600").compareTo(r.path("data").path("amount").decimalValue()), "USER cannot set the amount");

        // Picked the wrong person: she can move her record to the right employee.
        Employee right = employee("U002", "Right Person");
        r = json(put(alice, johnMeal, "{\"mealStatus\":\"ATE\",\"amount\":600,\"employeeId\":" + right.getId() + "}"));
        assertEquals(right.getId().longValue(), r.path("data").path("employeeId").asLong());

        // But she cannot rename an employee she did not create...
        HttpResponse<String> rename = send("PUT", "/meals/" + johnMeal, alice,
                "{\"mealStatus\":\"ATE\",\"amount\":600,\"employeeName\":\"Hacked Name\"}");
        assertEquals(403, rename.statusCode(), rename.body());
        assertEquals("Right Person", employeeRepository.findById(right.getId()).orElseThrow().getFullName());

        // ...and Bob cannot edit Alice's record at all; she cannot edit Bob's.
        assertEquals(403, send("PUT", "/meals/" + johnMeal, bob, "{\"mealStatus\":\"DID_NOT_EAT\",\"amount\":0}").statusCode());
        long bobs = meal(bob, employee("U003", "Bob Pick"), "2026-10-05", "ATE", null);
        assertEquals(403, send("PUT", "/meals/" + bobs, alice, "{\"mealStatus\":\"DID_NOT_EAT\",\"amount\":0}").statusCode());
        // ...and USER still has no other roles' functions.
        assertEquals(403, send("PUT", "/employees/" + right.getId(), alice, "{\"employeeName\":\"X\"}").statusCode());
        assertEquals(403, get("/trash", alice).statusCode());

        assertAudit("UPDATE_EMPLOYEE", "alice", "employee name 'John Mugisha' -> 'John Mugisha N.'");
        assertAudit("UPDATE_MEAL_RECORD", "alice", "status ATE -> DID_NOT_EAT");
    }

    // ---------------------------------------------------------------- helpers

    private Employee employee(String code, String name) {
        int sp = name.indexOf(' ');
        return employeeRepository.save(Employee.builder()
                .employeeCode(code).firstName(name.substring(0, sp)).lastName(name.substring(sp + 1))
                .department("Production").position("Worker").phone("")
                .status(EmployeeStatus.ACTIVE).build());
    }

    private long meal(String token, Employee e, String date, String status, String amount) throws Exception {
        HttpResponse<String> res = send("POST", "/meals", token, "{\"employeeId\":" + e.getId() + ",\"mealDate\":\"" + date
                + "\",\"mealStatus\":\"" + status + "\"" + (amount != null ? ",\"amount\":" + amount : "") + "}");
        assertEquals(201, res.statusCode(), res.body());
        return json(res).path("data").path("id").asLong();
    }

    private HttpResponse<String> put(String token, long id, String body) throws Exception {
        HttpResponse<String> res = send("PUT", "/meals/" + id, token, body);
        assertEquals(200, res.statusCode(), res.body());
        return res;
    }

    private JsonNode trash(String type) throws Exception {
        HttpResponse<String> res = get("/trash?size=200" + (type == null ? "" : "&type=" + type), admin);
        assertEquals(200, res.statusCode(), res.body());
        return json(res).path("data").path("content");
    }

    private JsonNode report(String from, String to) throws Exception {
        HttpResponse<String> res = get("/reports/daily?startDate=" + from + "&endDate=" + to, admin);
        assertEquals(200, res.statusCode(), res.body());
        return json(res).path("data");
    }

    /** October report and dashboard must both show exactly this ATE total and count. */
    private void assertReport(long amount, int ateCount) throws Exception {
        JsonNode rep = report("2026-10-01", "2026-10-31");
        assertEquals(0, BigDecimal.valueOf(amount).compareTo(rep.path("totalExpenditure").decimalValue()), rep.path("totalExpenditure").asText());
        assertEquals(ateCount, rep.path("ateCount").asInt());
        JsonNode dash = json(get("/dashboard/statistics", admin)).path("data");
        assertEquals(0, BigDecimal.valueOf(amount).compareTo(dash.path("totalExpenseCost").decimalValue()));
        assertEquals(ateCount, dash.path("totalMealsCount").asInt());
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private void assertAudit(String action, String username, String contains) {
        List<String> found = new ArrayList<>(jdbcTemplate.queryForList(
                "SELECT description FROM audit_logs WHERE action = ? AND username = ?", String.class, action, username));
        assertTrue(found.stream().anyMatch(d -> d.contains(contains)),
                action + " by " + username + " containing '" + contains + "' not found in " + found);
    }
}
