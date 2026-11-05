package com.emeal.http;

import com.emeal.entity.Role;
import com.emeal.entity.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clean start -> import September -> import October -> reconcile every figure, over real HTTP.
 *
 * <p>Expected counts and amounts are computed independently here, from the cells written into
 * the generated workbooks, and compared with what the Reports API, the PDF and Excel exports,
 * the dashboard and the expenses page return. The import uses PostgreSQL upserts, so this test
 * runs only against PostgreSQL (skipped on H2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class HistoricalImportReconciliationHttpTest extends HttpTestSupport {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    private String admin;

    /** Expected records per month: "name|yyyy-mm-dd" -> amount. */
    private final Map<String, BigDecimal> expectedSep = new TreeMap<>();
    private final Map<String, BigDecimal> expectedOct = new TreeMap<>();

    @BeforeEach
    void setUp() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            Assumptions.assumeTrue(c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres"),
                    "Excel import uses PostgreSQL upserts; run the suite against a disposable PostgreSQL database");
        }
        jdbc.update("DELETE FROM audit_logs");
        createUser("admin1", Role.ADMIN, UserStatus.ACTIVE);
        createUser("clerk1", Role.USER, UserStatus.ACTIVE);
        admin = login("admin1");
    }

    @Test
    void cleanStart_thenSeptemberAndOctoberImports_reconcileEverywhere() throws Exception {
        // ---------- 0. Old data that the clean start must remove (with a backup first) ----------
        jdbc.update("INSERT INTO employees (employee_code, first_name, last_name, department, position, phone, status, created_at, updated_at) " +
                "VALUES ('EMP001','Alice','Uwase','Production','Operator','', 'ACTIVE', NOW(), NOW())");
        long alice = jdbc.queryForObject("SELECT id FROM employees WHERE employee_code='EMP001'", Long.class);
        jdbc.update("INSERT INTO meal_records (employee_id, meal_date, meal_status, amount, recorded_by, created_at, updated_at) VALUES " +
                "(?, '2026-08-30', 'ATE', 600, 'old', NOW(), NOW()), (?, '2026-09-02', 'DID_NOT_EAT', 0, 'old', NOW(), NOW())", alice, alice);

        // USER may never reach the maintenance endpoints.
        String clerk = login("clerk1");
        assertEquals(403, get("/meals/maintenance/summary", clerk).statusCode());
        assertEquals(403, send("DELETE", "/meals/maintenance?expectedCount=2&confirm=DELETE", clerk, null).statusCode());

        JsonNode before = json(get("/meals/maintenance/summary", admin)).path("data");
        assertEquals(2, before.path("totalRecords").asLong());

        HttpResponse<byte[]> backup = binary("/meals/maintenance/backup", admin);
        assertEquals(200, backup.statusCode());
        String csv = new String(backup.body(), StandardCharsets.UTF_8);
        assertEquals(3, csv.strip().split("\n").length, "header + 2 records");
        assertTrue(csv.contains("2026-08-30,ATE,600.00,old") && csv.contains("2026-09-02,DID_NOT_EAT,0.00,old"), csv);

        // Wrong confirmation or a stale count deletes nothing.
        assertEquals(400, send("DELETE", "/meals/maintenance?expectedCount=2&confirm=delete", admin, null).statusCode());
        assertEquals(400, send("DELETE", "/meals/maintenance?expectedCount=1&confirm=DELETE", admin, null).statusCode());
        assertEquals(2, mealRecordRepository.count());

        HttpResponse<String> del = send("DELETE", "/meals/maintenance?expectedCount=2&confirm=DELETE", admin, null);
        assertEquals(200, del.statusCode(), del.body());
        assertEquals(0, mealRecordRepository.count());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action='BULK_DELETE_MEAL_RECORDS' AND username='admin1'", Integer.class));

        // ---------- 1. September: day-number headers, month from the sheet name ----------
        HttpResponse<String> sep = upload("september.xlsx", septemberWorkbook());
        assertEquals(200, sep.statusCode(), sep.body());
        JsonNode sepRes = json(sep).path("data");
        assertEquals(expectedSep.size(), sepRes.path("ateRecords").asInt());
        assertEquals(0, sepRes.path("didNotEatCount").asInt());
        assertEquals(0, sum(expectedSep).compareTo(new BigDecimal(sepRes.path("totalAmount").asText())));
        assertEquals("2026-09-01", sepRes.path("firstMealDate").asText());
        assertEquals("2026-09-30", sepRes.path("lastMealDate").asText());

        assertStored(expectedSep, "2026-09-01", "2026-09-30");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM meal_records WHERE meal_status <> 'ATE'", Integer.class));
        // created_at / imported_at are the import moment, meal_date is the actual day.
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM meal_records WHERE source <> 'IMPORT' OR imported_at IS NULL OR created_at IS NULL", Integer.class));
        LocalDate createdDay = jdbc.queryForObject("SELECT MIN(created_at) FROM meal_records", LocalDateTime.class).toLocalDate();
        assertTrue(createdDay.isAfter(LocalDate.of(2026, 9, 30)) || createdDay.equals(LocalDate.now()),
                "created_at must be the import time, not the meal date");

        JsonNode sepReport = report("startDate=2026-09-01&endDate=2026-09-30");
        assertReport(sepReport, expectedSep);

        // ---------- 2. October: real Excel date cells + text dates on an unnamed sheet ----------
        HttpResponse<String> oct = upload("october.xlsx", octoberWorkbook());
        assertEquals(200, oct.statusCode(), oct.body());
        JsonNode octRes = json(oct).path("data");
        assertEquals(expectedOct.size(), octRes.path("ateRecords").asInt());
        assertEquals(expectedOct.size(), octRes.path("recordsCreated").asInt());
        assertEquals("2026-10-01", octRes.path("firstMealDate").asText());
        assertEquals("2026-10-31", octRes.path("lastMealDate").asText());
        assertStored(expectedOct, "2026-10-01", "2026-10-31");

        JsonNode octReport = report("startDate=2026-10-01&endDate=2026-10-31");
        assertReport(octReport, expectedOct);
        assertReport(report("startDate=2026-09-01&endDate=2026-09-30"), expectedSep); // unchanged by October

        Map<String, BigDecimal> both = new TreeMap<>(expectedSep);
        both.putAll(expectedOct);
        assertEquals(expectedSep.size() + expectedOct.size(), both.size(), "months must not overlap");
        JsonNode bothReport = report("startDate=2026-09-01&endDate=2026-10-31");
        assertReport(bothReport, both);
        BigDecimal grand = sum(both);
        assertEquals(0, grand.compareTo(sum(expectedSep).add(sum(expectedOct))));

        System.out.printf("RECONCILE September: expected %d meals / %s RWF | report %s / %s%n", expectedSep.size(), sum(expectedSep),
                sepReport.path("ateCount").asText(), sepReport.path("totalExpenditure").asText());
        System.out.printf("RECONCILE October:   expected %d meals / %s RWF | report %s / %s%n", expectedOct.size(), sum(expectedOct),
                octReport.path("ateCount").asText(), octReport.path("totalExpenditure").asText());
        System.out.printf("RECONCILE Sep+Oct:   expected %d meals / %s RWF | report %s / %s%n", both.size(), grand,
                bothReport.path("ateCount").asText(), bothReport.path("totalExpenditure").asText());

        // ---------- 3. Exports, dashboard and expenses show the same figures ----------
        for (String[] range : new String[][]{{"2026-09-01", "2026-09-30"}, {"2026-10-01", "2026-10-31"}, {"2026-09-01", "2026-10-31"}}) {
            Map<String, BigDecimal> exp = range[0].equals("2026-09-01") && range[1].equals("2026-09-30") ? expectedSep
                    : range[0].equals("2026-10-01") ? expectedOct : both;
            String q = "startDate=" + range[0] + "&endDate=" + range[1];
            assertExcel(binary("/reports/daily/excel?" + q, admin), exp);
            assertPdf(binary("/reports/daily/pdf?" + q, admin), exp);
        }

        JsonNode dash = json(get("/dashboard/statistics", admin)).path("data");
        assertEquals(both.size(), dash.path("totalMealsCount").asLong());
        assertEquals(0, grand.compareTo(new BigDecimal(dash.path("totalExpenseCost").asText())));

        JsonNode expenses = json(get("/expenses/summary", admin)).path("data");
        System.out.printf("RECONCILE dashboard: %s meals / %s RWF | expenses all-time %s, October %s%n",
                dash.path("totalMealsCount").asText(), dash.path("totalExpenseCost").asText(),
                expenses.path("totalExpense").asText(), expenses.path("monthlyExpense").asText());
        assertEquals(0, grand.compareTo(new BigDecimal(expenses.path("totalExpense").asText())));
        // Latest meal day is 31/10, so "monthly" is October only.
        assertEquals(0, sum(expectedOct).compareTo(new BigDecimal(expenses.path("monthlyExpense").asText())));

        // ---------- 4. Re-importing September updates the same rows: no duplicates ----------
        HttpResponse<String> again = upload("september.xlsx", septemberWorkbook());
        assertEquals(200, again.statusCode(), again.body());
        assertEquals(expectedSep.size(), json(again).path("data").path("recordsUpdated").asInt());
        assertEquals(0, json(again).path("data").path("recordsCreated").asInt());
        assertEquals(both.size(), mealRecordRepository.count());
        assertReport(report("startDate=2026-09-01&endDate=2026-10-31"), both);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM (SELECT employee_id, meal_date FROM meal_records GROUP BY 1, 2 HAVING COUNT(*) > 1) d", Integer.class));
    }

    @Test
    void unreadableValuesBlockTheWholeImport() throws Exception {
        long before = mealRecordRepository.count();

        // A European "1.500" (1.5 RWF) and a stray word must be flagged, not guessed.
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet("September 2026");
        header(s, "Names", "1", "2", "3");
        Row r = s.createRow(1);
        r.createCell(0).setCellValue("Bad Values");
        r.createCell(1).setCellValue(1500);
        r.createCell(2).setCellValue("1.500");
        r.createCell(3).setCellValue("maybe");
        HttpResponse<String> res = upload("bad.xlsx", bytes(wb));
        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("Nothing was imported"), res.body());
        assertTrue(res.body().contains("C2") && res.body().contains("D2"), res.body());
        assertEquals(before, mealRecordRepository.count());
        assertEquals(0, employeeRepository.count(), "no employee is created by a rejected import");

        // Preview lists the same problems without writing anything.
        HttpResponse<String> preview = multipart("/employees/import/preview", "bad.xlsx", bytes(wb));
        assertEquals(200, preview.statusCode(), preview.body());
        assertEquals(2, json(preview).path("data").path("invalidRows").asInt());

        // Rows sent back from a preview (a sample) can never be imported as if they were the file.
        HttpResponse<String> partial = send("POST", "/employees/import/confirm", admin,
                "{\"mealDate\":\"2026-10-01\",\"rows\":[{\"rowNumber\":1,\"employeeName\":\"Sample Person\",\"mealDate\":\"2026-10-01\",\"mealStatus\":\"Ate\",\"amountUsed\":600,\"status\":\"VALID\"}]}");
        assertEquals(400, partial.statusCode(), partial.body());
        assertTrue(partial.body().contains("Partial imports are not allowed"), partial.body());
        assertEquals(before, mealRecordRepository.count());

        // Day numbers without any month must never fall back to the current month.
        XSSFWorkbook noMonth = new XSSFWorkbook();
        Sheet s2 = noMonth.createSheet("Sheet1");
        header(s2, "Names", "1", "2");
        Row r2 = s2.createRow(1);
        r2.createCell(0).setCellValue("Someone");
        r2.createCell(1).setCellValue(600);
        HttpResponse<String> res2 = upload("nomonth.xlsx", bytes(noMonth));
        assertEquals(400, res2.statusCode(), res2.body());
        assertTrue(res2.body().contains("no month"), res2.body());
        assertEquals(before, mealRecordRepository.count());
    }

    @Test
    void ambiguousFirstNameIsRejectedInsteadOfGuessingTheEmployee() throws Exception {
        jdbc.update("INSERT INTO employees (employee_code, first_name, last_name, department, position, phone, status, created_at, updated_at) VALUES " +
                "('EMP001','Jean','Bosco','General','Worker','', 'ACTIVE', NOW(), NOW()), ('EMP002','Jean','Paul','General','Worker','', 'ACTIVE', NOW(), NOW())");
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet("October 2026");
        header(s, "Names", "1", "2");
        Row r = s.createRow(1);
        r.createCell(0).setCellValue("Jean");
        r.createCell(1).setCellValue(600);
        HttpResponse<String> res = upload("ambiguous.xlsx", bytes(wb));
        assertEquals(400, res.statusCode(), res.body());
        assertTrue(res.body().contains("matches more than one employee"), res.body());
        assertEquals(0, mealRecordRepository.count());

        // The full name resolves to exactly one employee.
        r.getCell(0).setCellValue("Jean Paul");
        HttpResponse<String> ok = upload("ok.xlsx", bytes(wb));
        assertEquals(200, ok.statusCode(), ok.body());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM meal_records m JOIN employees e ON e.id = m.employee_id " +
                "WHERE e.employee_code = 'EMP002' AND m.meal_date = '2026-10-01' AND m.amount = 600", Integer.class));
        assertEquals(2, employeeRepository.count(), "no new employee for a matched name");
    }

    // ======================================================================
    // Workbooks (expected values are recorded while the cells are written)
    // ======================================================================

    private static final String[] PEOPLE = {"Alice Uwase", "Eric Habimana", "Grace Mukamana", "Patrick Niyonzima", "Diane Ingabire"};

    /** Sheet "September 2026", headers "Names", 1..30, "Total"; varied cell styles; a totals row. */
    private byte[] septemberWorkbook() throws Exception {
        expectedSep.clear();
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet("September 2026");
        Row h = s.createRow(0);
        h.createCell(0).setCellValue("Names");
        for (int d = 1; d <= 30; d++) h.createCell(d).setCellValue(d);   // numeric day headers
        h.createCell(31).setCellValue("Total");

        for (int p = 0; p < PEOPLE.length; p++) {
            Row r = s.createRow(p + 1);
            r.createCell(0).setCellValue(PEOPLE[p]);
            BigDecimal rowTotal = BigDecimal.ZERO;
            for (int d = 1; d <= 30; d++) {
                int k = (p * 7 + d) % 6;
                Cell c = r.createCell(d);
                String key = PEOPLE[p] + "|" + LocalDate.of(2026, 9, d);
                switch (k) {
                    case 0 -> { c.setCellValue(1500); expectedSep.put(key, new BigDecimal("1500")); }
                    case 1 -> { c.setCellValue("1,500"); expectedSep.put(key, new BigDecimal("1500")); }
                    case 2 -> { c.setCellValue("ate"); expectedSep.put(key, new BigDecimal("600")); } // standard price
                    case 3 -> { /* empty: no meal, nothing recorded */ }
                    case 4 -> c.setCellValue("-");
                    default -> { c.setCellValue(600); expectedSep.put(key, new BigDecimal("600")); }
                }
                if (expectedSep.containsKey(key)) rowTotal = rowTotal.add(expectedSep.get(key));
            }
            r.createCell(31).setCellValue(rowTotal.doubleValue()); // must not be imported
        }
        // An employee with no meals at all, and a totals row without a name.
        s.createRow(PEOPLE.length + 1).createCell(0).setCellValue("No Meals Person");
        Row totals = s.createRow(PEOPLE.length + 3);
        totals.createCell(0).setCellValue("TOTAL");
        for (int d = 1; d <= 30; d++) totals.createCell(d).setCellValue(9999);
        Row nameless = s.createRow(PEOPLE.length + 4);
        nameless.createCell(5).setCellValue(4200);
        return bytes(wb);
    }

    /** Sheet "Sheet1" (no month in the name): real Excel date cells, plus two text-date columns. */
    private byte[] octoberWorkbook() throws Exception {
        expectedOct.clear();
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
        Sheet s = wb.createSheet("Sheet1");
        s.createRow(0).createCell(0).setCellValue("MEAL ATTENDANCE OCTOBER 2026"); // title row above the header
        Row h = s.createRow(1);
        h.createCell(0).setCellValue("No");
        h.createCell(1).setCellValue("Employee Name");
        for (int d = 1; d <= 29; d++) {
            Cell c = h.createCell(d + 1);
            c.setCellValue(LocalDate.of(2026, 10, d));
            c.setCellStyle(dateStyle);
        }
        h.createCell(31).setCellValue("30th Oct");
        h.createCell(32).setCellValue("31/10/2026");
        h.createCell(33).setCellValue("Amount Paid");

        // As written in the file / as stored: a reversed name matches the existing employee.
        String[] written = {PEOPLE[0], "Habimana Eric", "New Comer"};
        String[] people = {PEOPLE[0], PEOPLE[1], "New Comer"};
        for (int p = 0; p < people.length; p++) {
            Row r = s.createRow(p + 2);
            r.createCell(0).setCellValue(p + 1);
            r.createCell(1).setCellValue(written[p]);
            for (int d = 1; d <= 31; d++) {
                if ((p + d) % 3 == 0) continue; // no meal
                int amount = (d % 2 == 0) ? 1500 : 600;
                r.createCell(d + 1).setCellValue(amount);
                expectedOct.put(people[p] + "|" + LocalDate.of(2026, 10, d), new BigDecimal(amount));
            }
            r.createCell(33).setCellValue(123456); // "Amount Paid" column is ignored
        }
        return bytes(wb);
    }

    // ======================================================================
    // Assertions
    // ======================================================================

    private void assertStored(Map<String, BigDecimal> expected, String from, String to) {
        Map<String, BigDecimal> stored = new TreeMap<>();
        jdbc.query("SELECT TRIM(e.first_name || ' ' || e.last_name), m.meal_date, m.amount, m.meal_status FROM meal_records m " +
                        "JOIN employees e ON e.id = m.employee_id WHERE m.meal_date BETWEEN CAST(? AS DATE) AND CAST(? AS DATE)",
                rs -> {
                    assertEquals("ATE", rs.getString(4));
                    stored.put(rs.getString(1) + "|" + rs.getDate(2).toLocalDate(), rs.getBigDecimal(3).stripTrailingZeros());
                }, from, to);
        Map<String, BigDecimal> exp = new TreeMap<>();
        expected.forEach((k, v) -> exp.put(k, v.stripTrailingZeros()));
        assertEquals(exp, stored);
    }

    private void assertReport(JsonNode report, Map<String, BigDecimal> expected) {
        assertEquals(expected.size(), report.path("ateCount").asLong());
        assertEquals(0, report.path("didNotEatCount").asLong());
        assertEquals(0, sum(expected).compareTo(new BigDecimal(report.path("totalExpenditure").asText())),
                "report total " + report.path("totalExpenditure").asText() + " vs expected " + sum(expected));
        assertEquals(expected.size(), report.path("records").size());
        Set<String> seen = new HashSet<>();
        BigDecimal recordSum = BigDecimal.ZERO;
        for (JsonNode r : report.path("records")) {
            String key = r.path("employeeName").asText().trim() + "|" + r.path("mealDate").asText();
            assertTrue(seen.add(key), "duplicate record " + key);
            assertTrue(expected.containsKey(key), "unexpected record " + key);
            assertEquals(0, expected.get(key).compareTo(new BigDecimal(r.path("amount").asText())), key);
            recordSum = recordSum.add(new BigDecimal(r.path("amount").asText()));
        }
        assertEquals(0, recordSum.compareTo(sum(expected)));
    }

    private void assertExcel(HttpResponse<byte[]> res, Map<String, BigDecimal> expected) throws Exception {
        assertEquals(200, res.statusCode());
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(res.body()))) {
            DataFormatter f = new DataFormatter();
            int rows = 0;
            BigDecimal rowSum = BigDecimal.ZERO;
            BigDecimal totalCell = null;
            String totalLabel = null;
            for (Row r : wb.getSheetAt(0)) {
                String first = r.getCell(0) == null ? "" : f.formatCellValue(r.getCell(0));
                if (first.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    rows++;
                    rowSum = rowSum.add(BigDecimal.valueOf(r.getCell(6).getNumericCellValue()));
                } else if (first.equals("TOTAL EXPENDITURE")) {
                    totalCell = BigDecimal.valueOf(r.getCell(6).getNumericCellValue());
                } else if (first.equals("Total Expenditure:")) {
                    totalLabel = f.formatCellValue(r.getCell(1));
                }
            }
            assertEquals(expected.size(), rows);
            assertEquals(0, sum(expected).compareTo(rowSum));
            assertNotNull(totalCell);
            assertEquals(0, sum(expected).compareTo(totalCell));
            assertEquals(sum(expected).setScale(2).toPlainString() + " RWF", totalLabel);
        }
    }

    private void assertPdf(HttpResponse<byte[]> res, Map<String, BigDecimal> expected) throws Exception {
        assertEquals(200, res.statusCode());
        PdfReader reader = new PdfReader(res.body());
        StringBuilder sb = new StringBuilder();
        PdfTextExtractor ex = new PdfTextExtractor(reader);
        for (int p = 1; p <= reader.getNumberOfPages(); p++) sb.append(ex.getTextFromPage(p));
        String text = sb.toString().replaceAll("\\s+", "");
        assertTrue(text.contains("TotalExpenditure:" + sum(expected).setScale(2).toPlainString() + "RWF"), "PDF total missing");
        assertTrue(text.contains("RecordedAte:" + expected.size()), "PDF count missing");
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private static BigDecimal sum(Map<String, BigDecimal> m) {
        return m.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void header(Sheet s, String... values) {
        Row h = s.createRow(0);
        for (int i = 0; i < values.length; i++) h.createCell(i).setCellValue(values[i]);
    }

    private static byte[] bytes(Workbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private JsonNode report(String query) throws Exception {
        HttpResponse<String> res = get("/reports/daily?" + query, admin);
        assertEquals(200, res.statusCode(), res.body());
        return json(res).path("data");
    }

    private HttpResponse<byte[]> binary(String path, String token) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                .header("Authorization", "Bearer " + token).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    /** Uploads like the app does, with the import day (not the meal month) as the chosen date. */
    private HttpResponse<String> upload(String filename, byte[] content) throws Exception {
        return multipart("/employees/import/upload?mealDate=2026-11-05", filename, content);
    }

    private HttpResponse<String> multipart(String path, String filename, byte[] content) throws Exception {
        String boundary = "----test" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                .header("Authorization", "Bearer " + admin)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
