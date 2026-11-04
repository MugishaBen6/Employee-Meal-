package com.emeal.http;

import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.entity.Role;
import com.emeal.entity.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * From Date / To Date report filtering over real HTTP: the JSON report, the Excel export and the
 * PDF export must contain exactly the records whose meal date lies inside the inclusive period.
 *
 * Data: Q1 31/08, Q1 01/09 (ATE), Q2 01/09 (DID_NOT_EAT), Q3 15/09, Q2 30/09, Q1 01/10.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class ReportDateRangeHttpTest extends HttpTestSupport {

    private String token;

    @BeforeEach
    void seed() throws Exception {
        createUser("hr1", Role.HR, UserStatus.ACTIVE);
        token = login("hr1");
        Employee q1 = employee("Q1", "Alice", "Production", "Machine Operator");
        Employee q2 = employee("Q2", "Eric", "Production", "Driver");
        Employee q3 = employee("Q3", "Jean", "Finance", "Accountant");
        meal(q1, "2026-08-31", MealStatus.ATE, "600.00");          // day before the month
        meal(q1, "2026-09-01", MealStatus.ATE, "600.00");
        meal(q2, "2026-09-01", MealStatus.DID_NOT_EAT, "0.00");
        meal(q3, "2026-09-15", MealStatus.ATE, "600.00");
        meal(q2, "2026-09-30", MealStatus.ATE, "600.00");          // last day of the month
        meal(q1, "2026-10-01", MealStatus.ATE, "600.00");          // day after the month
    }

    // ---------- JSON report ----------

    @Test
    void sameFromAndToDate_returnsOnlyThatDay() throws Exception {
        JsonNode s = report("startDate=2026-09-01&endDate=2026-09-01");

        assertEquals(List.of("2026-09-01|Q1", "2026-09-01|Q2"), keys(s.path("records")));
        assertTrue(s.path("singleDay").asBoolean());
        assertEquals("01/09/2026", s.path("formattedReportDate").asText());
        assertEquals(1, s.path("ateCount").asInt());
        assertEquals(1, s.path("didNotEatCount").asInt());
        assertEquals(0, new BigDecimal("600").compareTo(s.path("totalExpenditure").decimalValue()));
    }

    @Test
    void wholeMonth_includesBothBoundaryDays_andExcludesOutsideRecords() throws Exception {
        JsonNode s = report("startDate=2026-09-01&endDate=2026-09-30");

        // Sorted by meal date, then employee code. 31/08 and 01/10 are excluded.
        assertEquals(List.of("2026-09-01|Q1", "2026-09-01|Q2", "2026-09-15|Q3", "2026-09-30|Q2"),
                keys(s.path("records")));
        assertFalse(s.path("singleDay").asBoolean());
        assertEquals("01/09/2026 - 30/09/2026", s.path("formattedReportDate").asText());
        assertEquals("2026-09-01", s.path("startDate").asText());
        assertEquals("2026-09-30", s.path("endDate").asText());
        assertEquals(3, s.path("ateCount").asInt());
        assertEquals(1, s.path("didNotEatCount").asInt());
        assertEquals(0, new BigDecimal("1800").compareTo(s.path("totalExpenditure").decimalValue()));
    }

    @Test
    void departmentFilter_appliesWithinThePeriod() throws Exception {
        assertEquals(List.of("2026-09-15|Q3"),
                keys(report("startDate=2026-09-01&endDate=2026-09-30&department=Finance").path("records")));
        assertEquals(List.of("2026-09-01|Q1", "2026-09-01|Q2", "2026-09-30|Q2"),
                keys(report("startDate=2026-09-01&endDate=2026-09-30&department=Production").path("records")));
    }

    @Test
    void legacySingleDateParameter_stillWorks_andOnlyOneBoundIsASingleDay() throws Exception {
        assertEquals(List.of("2026-09-01|Q1", "2026-09-01|Q2"), keys(report("date=2026-09-01").path("records")));
        assertEquals(List.of("2026-09-30|Q2"), keys(report("startDate=2026-09-30").path("records")));
        assertEquals(List.of("2026-10-01|Q1"), keys(report("endDate=2026-10-01").path("records")));
        // An empty period is valid and returns no records.
        assertEquals(0, report("startDate=2026-09-02&endDate=2026-09-14").path("records").size());
    }

    @Test
    void invalidPeriods_areRejectedWith400() throws Exception {
        HttpResponse<String> reversed = get("/reports/daily?startDate=2026-09-30&endDate=2026-09-01", token);
        assertEquals(400, reversed.statusCode());
        assertSafeErrorBody(reversed);
        assertEquals("From Date must be on or before To Date.", json(reversed).path("message").asText());

        HttpResponse<String> tooLong = get("/reports/daily?startDate=2025-01-01&endDate=2026-09-30", token);
        assertEquals(400, tooLong.statusCode());
        assertSafeErrorBody(tooLong);

        assertEquals(400, get("/reports/daily?startDate=2026-02-30&endDate=2026-03-01", token).statusCode());
        assertEquals(400, binary("/reports/daily/excel?startDate=2026-09-30&endDate=2026-09-01").statusCode());
        assertEquals(400, binary("/reports/daily/pdf?startDate=2026-09-30&endDate=2026-09-01").statusCode());
    }

    // ---------- Exports contain exactly the same records as the page ----------

    @Test
    void excelExport_matchesJsonRecordsExactly() throws Exception {
        for (String q : new String[]{"startDate=2026-09-01&endDate=2026-09-01",
                "startDate=2026-09-01&endDate=2026-09-30",
                "startDate=2026-09-01&endDate=2026-09-30&department=Production"}) {
            List<String> expected = keys(report(q).path("records"));

            HttpResponse<byte[]> res = binary("/reports/daily/excel?" + q);
            assertEquals(200, res.statusCode());
            List<String> rows = new ArrayList<>();
            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(res.body()))) {
                DataFormatter f = new DataFormatter();
                for (Row r : wb.getSheetAt(0)) {
                    Cell first = r.getCell(0);
                    String date = first == null ? "" : f.formatCellValue(first);
                    if (date.matches("\\d{2}/\\d{2}/\\d{4}")) {   // data rows start with the meal date
                        rows.add(toIso(date) + "|" + f.formatCellValue(r.getCell(1)));
                    }
                }
            }
            assertEquals(expected, rows, "Excel rows must equal the report records for " + q);
        }
        HttpResponse<byte[]> month = binary("/reports/daily/excel?startDate=2026-09-01&endDate=2026-09-30");
        assertEquals("attachment; filename=\"Meal_Report_2026-09-01_to_2026-09-30.xlsx\"",
                month.headers().firstValue("Content-Disposition").orElse(""));
        HttpResponse<byte[]> day = binary("/reports/daily/excel?startDate=2026-09-01&endDate=2026-09-01");
        assertEquals("attachment; filename=\"Daily_Meal_Report_2026-09-01.xlsx\"",
                day.headers().firstValue("Content-Disposition").orElse(""));
    }

    @Test
    void pdfExport_matchesJsonRecordsExactly() throws Exception {
        HttpResponse<byte[]> res = binary("/reports/daily/pdf?startDate=2026-09-01&endDate=2026-09-30");
        assertEquals(200, res.statusCode());
        assertEquals("attachment; filename=\"Meal_Report_2026-09-01_to_2026-09-30.pdf\"",
                res.headers().firstValue("Content-Disposition").orElse(""));

        PdfReader reader = new PdfReader(res.body());
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int p = 1; p <= reader.getNumberOfPages(); p++) {
            sb.append(extractor.getTextFromPage(p));
        }
        String text = sb.toString().replaceAll("\\s+", ""); // table cells may wrap

        assertTrue(text.contains("ReportPeriod:01/09/2026-30/09/2026"), text);
        // Q1 has records on 31/08, 01/09 and 01/10, but only 01/09 is in the period.
        assertEquals(1, count(text, "Q1"));
        assertEquals(2, count(text, "Q2"));
        assertEquals(1, count(text, "Q3"));
        assertFalse(text.contains("31/08/2026"), "record before the period must not be exported");
        assertEquals(2, count(text, "01/09/2026") - 1, "two records on 01/09 (plus the period label)");
        assertTrue(text.contains("15/09/2026"));
        assertTrue(text.contains("TotalExpenditure:1800.00RWF"), text);
    }

    @Test
    void reportPermissions_areUnchanged() throws Exception {
        createUser("alice", Role.USER, UserStatus.ACTIVE);
        String user = login("alice");
        assertEquals(403, get("/reports/daily?startDate=2026-09-01&endDate=2026-09-30", user).statusCode());
        assertEquals(401, get("/reports/daily?startDate=2026-09-01&endDate=2026-09-30", null).statusCode());
        for (Role role : new Role[]{Role.ADMIN, Role.MANAGING_DIRECTOR, Role.ACCOUNTANT}) {
            String name = "r_" + role.name().toLowerCase();
            createUser(name, role, UserStatus.ACTIVE);
            assertEquals(200, get("/reports/daily?startDate=2026-09-01&endDate=2026-09-30", login(name)).statusCode(), role.name());
        }
    }

    // ---------- helpers ----------

    private JsonNode report(String query) throws Exception {
        HttpResponse<String> res = get("/reports/daily?" + query, token);
        assertEquals(200, res.statusCode(), res.body());
        return json(res).path("data");
    }

    private HttpResponse<byte[]> binary(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                .header("Authorization", "Bearer " + token).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static List<String> keys(JsonNode records) {
        List<String> out = new ArrayList<>();
        records.forEach(r -> out.add(r.path("mealDate").asText() + "|" + r.path("employeeCode").asText()));
        return out;
    }

    private static String toIso(String ddMMyyyy) {
        return ddMMyyyy.substring(6, 10) + "-" + ddMMyyyy.substring(3, 5) + "-" + ddMMyyyy.substring(0, 2);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    private Employee employee(String code, String name, String department, String position) {
        return employeeRepository.save(Employee.builder()
                .employeeCode(code).firstName(name).lastName("Test")
                .department(department).position(position).phone("+25078855" + code.charAt(1) + "000")
                .status(EmployeeStatus.ACTIVE).build());
    }

    private void meal(Employee emp, String date, MealStatus status, String amount) {
        mealRecordRepository.save(MealRecord.builder()
                .employee(emp).mealDate(LocalDate.parse(date)).mealStatus(status)
                .amount(new BigDecimal(amount)).recordedBy("hr1").build());
    }
}
