package com.emeal.service;

import com.emeal.config.BusinessTime;

import com.emeal.dto.excel.*;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import com.emeal.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExcelImportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelImportService.class);

    private final EmployeeRepository employeeRepository;
    private final MealRecordRepository mealRecordRepository;
    private final AuditLogService auditLogService;
    private final SettingsService settingsService;
    private final JdbcTemplate jdbcTemplate;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s-]{8,15}$");
    private static final Pattern EMP_CODE_PATTERN = Pattern.compile("EMP(\\d+)", Pattern.CASE_INSENSITIVE);

    public ExcelImportService(EmployeeRepository employeeRepository,
                              MealRecordRepository mealRecordRepository,
                              AuditLogService auditLogService,
                              SettingsService settingsService,
                              JdbcTemplate jdbcTemplate) {
        this.employeeRepository = employeeRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.auditLogService = auditLogService;
        this.settingsService = settingsService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Internal data structure representing a single parsed employee daily meal record.
     */
    public static class ParsedMealItem {
        public String employeeName;
        public LocalDate mealDate;
        public MealStatus mealStatus;
        public BigDecimal amount;
        public String sheetName;
        public int rowNumber;

        public ParsedMealItem(String employeeName, LocalDate mealDate, MealStatus mealStatus, BigDecimal amount, String sheetName, int rowNumber) {
            this.employeeName = employeeName;
            this.mealDate = mealDate;
            this.mealStatus = mealStatus;
            this.amount = amount;
            this.sheetName = sheetName;
            this.rowNumber = rowNumber;
        }
    }

    /**
     * Generates a clean, professional Excel template with standard columns and sample instructions.
     */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Import Template");

            // Header Font & Style
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Sample Data Style
            CellStyle sampleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font sampleFont = workbook.createFont();
            sampleFont.setItalic(true);
            sampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            sampleStyle.setFont(sampleFont);

            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            String[] headers = {"Employee Name", "Telephone", "Position", "Meal Status", "Amount Used"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example instructional rows
            String[][] sampleData = {
                {"John Doe", "0781234567", "Worker", "Ate", "600"},
                {"Jane Smith", "0791234567", "Manager", "Not Ate", "0"},
                {"Eric Mugisha", "0787654321", "Driver", "Ate", "600"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row dataRow = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = dataRow.createCell(c);
                    cell.setCellValue(sampleData[r][c]);
                    cell.setCellStyle(sampleStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 4200));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel template: " + e.getMessage(), e);
        }
    }

    /* ======================================================================
       Import entry points
       ----------------------------------------------------------------------
       Rules (all formats):
         * The meal date always comes from the file (header date / sheet month) or, for the
           single-day standard template, from the date the user explicitly chose. Never today.
         * Every imported meal record is ATE. Cells/rows meaning "no meal" create no record;
           imports never write DID_NOT_EAT.
         * Amounts are exact BigDecimal values. Anything that cannot be read with certainty
           (invalid amount, missing month, ambiguous employee name, conflicting duplicates)
           blocks the whole import and is listed, so totals are never silently wrong.
         * One record per employee per day (upsert on the unique key), so re-importing a file
           updates the same rows instead of duplicating them.
       ====================================================================== */

    @Transactional
    public ExcelImportResultResponse importExcelDirect(MultipartFile file, LocalDate defaultDate) {
        validateFile(file);
        // Attribute imported rows to the authenticated caller only; never fall back to a default identity.
        String recordedBy = SecurityUtils.getCurrentUsername();
        ImportPlan plan = readWorkbookPlan(file, defaultDate);
        return savePlan(plan, recordedBy);
    }

    /** Reads and validates the file exactly as an import would, without writing anything. */
    @Transactional(readOnly = true)
    public ExcelImportPreviewResponse previewExcel(MultipartFile file, LocalDate defaultDate) {
        validateFile(file);
        return buildPreview(readWorkbookPlan(file, defaultDate));
    }

    /**
     * Importing rows sent back by a client is refused: a preview holds only a sample of the file, so
     * confirming it saved a partial month while reporting success. Files are imported only whole,
     * through {@link #importExcelDirect}.
     */
    public ExcelImportResultResponse confirmImport(ExcelImportConfirmRequest request) {
        throw new BadRequestException("Partial imports are not allowed. Nothing was saved. "
                + "Please refresh the page (Ctrl+Shift+R) and upload the whole Excel file again.");
    }

    /* ======================================================================
       Reading the workbook
       ====================================================================== */

    private ImportPlan readWorkbookPlan(MultipartFile file, LocalDate chosenDate) {
        int defaultYear = (chosenDate != null ? chosenDate : BusinessTime.today()).getYear();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("The uploaded Excel workbook contains no sheets.");
            }
            DataFormatter formatter = new DataFormatter();
            BigDecimal standardPrice = settingsService.getStandardMealPrice();

            RawImport raw = parseAllMatrixSheets(workbook, defaultYear, formatter, standardPrice);
            if (raw.matrixFound) {
                return buildPlan(raw);
            }

            // Standard single-day template (Name, Telephone, Position, Meal Status, Amount):
            // it has no date column, so the date chosen by the user is the meal date.
            if (chosenDate == null) {
                throw new BadRequestException("This file has no date columns. Choose the meal date the sheet belongs to and try again.");
            }
            ExcelImportPreviewResponse std = parseStandardFormat(workbook.getSheetAt(0), chosenDate, formatter);
            return buildPlan(rawFromStandardRows(std.getRows(), chosenDate, standardPrice));
        } catch (BadRequestException | com.emeal.exception.MisconfigurationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to read Excel file ({})", e.getClass().getSimpleName());
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    /** Parsed content of a file before employee names are resolved. */
    private static final class RawImport {
        boolean matrixFound;
        final List<ParsedMealItem> meals = new ArrayList<>();
        final Map<String, String> names = new LinkedHashMap<>(); // name key -> display name
        final List<ExcelEmployeeRowDTO> problems = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        int noMealCells;
        int skippedRows;
    }

    /**
     * Matrix sheets: one row per employee, one column per day, cells hold the amount (or "ate").
     * Sheets without a date header row are ignored (e.g. a plain list of names).
     */
    private RawImport parseAllMatrixSheets(Workbook workbook, int defaultYear, DataFormatter formatter, BigDecimal standardPrice) {
        RawImport raw = new RawImport();

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                continue;
            }
            String sheetName = sheet.getSheetName();

            // ---- Find the header row: the first row with at least two date columns.
            int headerRowIndex = -1;
            int nameCol = -1;
            Map<Integer, LocalDate> dateCols = new TreeMap<>();
            Map<Integer, String> headerText = new HashMap<>();
            List<String> dayOnlyHeaders = new ArrayList<>();

            int scanTo = Math.min(sheet.getLastRowNum(), 9);
            for (int r = 0; r <= scanTo; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<Integer, LocalDate> dates = new TreeMap<>();
                Map<Integer, String> texts = new HashMap<>();
                List<String> dayOnly = new ArrayList<>();
                int foundName = -1;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    String val = formatter.formatCellValue(cell).trim();
                    if (val.isEmpty()) continue;
                    String key = val.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                    if (foundName < 0 && isNameHeader(key)) {
                        foundName = c;
                        continue;
                    }
                    if (key.contains("total") || key.contains("amount") || key.contains("summary")) {
                        continue;
                    }
                    MealImportParsing.HeaderDate hd = MealImportParsing.readHeaderDate(cell, val, defaultYear, sheetName);
                    if (hd.kind() == MealImportParsing.HeaderKind.DATE) {
                        dates.put(c, hd.date());
                        texts.put(c, val);
                    } else if (hd.kind() == MealImportParsing.HeaderKind.DAY_WITHOUT_MONTH) {
                        dayOnly.add(val);
                    }
                }
                if (dates.size() + dayOnly.size() >= 2) {
                    headerRowIndex = r;
                    nameCol = foundName;
                    dateCols = dates;
                    headerText = texts;
                    dayOnlyHeaders = dayOnly;
                    break;
                }
            }
            if (headerRowIndex < 0) {
                continue;
            }
            raw.matrixFound = true;
            int headerRowNumber = headerRowIndex + 1;

            if (!dayOnlyHeaders.isEmpty()) {
                raw.problems.add(problem(headerRowNumber, null, null, "Sheet '" + sheetName + "': the day columns ("
                        + String.join(", ", dayOnlyHeaders.subList(0, Math.min(5, dayOnlyHeaders.size())))
                        + (dayOnlyHeaders.size() > 5 ? ", …" : "")
                        + ") have no month, and the sheet name does not contain one. Rename the sheet to its month "
                        + "(for example 'September 2026') or use full dates in the header row."));
                continue;
            }

            Map<LocalDate, Integer> firstColForDate = new HashMap<>();
            boolean duplicateColumn = false;
            for (Map.Entry<Integer, LocalDate> e : dateCols.entrySet()) {
                Integer prev = firstColForDate.putIfAbsent(e.getValue(), e.getKey());
                if (prev != null) {
                    duplicateColumn = true;
                    raw.problems.add(problem(headerRowNumber, null, e.getValue().toString(), "Sheet '" + sheetName
                            + "': columns " + CellReference.convertNumToColString(prev) + " and "
                            + CellReference.convertNumToColString(e.getKey()) + " are both " + e.getValue() + "."));
                }
            }
            if (duplicateColumn) {
                continue;
            }

            if (nameCol < 0) {
                int firstDateCol = dateCols.keySet().iterator().next();
                if (firstDateCol == 0) {
                    raw.problems.add(problem(headerRowNumber, null, null, "Sheet '" + sheetName
                            + "': no employee name column was found. Add a 'Name' column before the date columns."));
                    continue;
                }
                nameCol = 0;
            }

            // ---- Data rows
            int namelessWithValues = 0;
            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int rowNumber = r + 1;
                String rawName = getCellString(row, nameCol, formatter).trim().replaceAll("\\s+", " ");

                if (rawName.isEmpty()) {
                    for (Integer col : dateCols.keySet()) {
                        if (MealImportParsing.readMealCell(row.getCell(col), formatter, standardPrice).kind() != MealImportParsing.Kind.NO_MEAL) {
                            namelessWithValues++;
                            break;
                        }
                    }
                    continue;
                }
                String lower = rawName.toLowerCase(Locale.ROOT);
                if (lower.startsWith("total") || lower.startsWith("grand total") || lower.startsWith("summary")
                        || lower.equals("names") || lower.equals("name")) {
                    continue;
                }
                String nameKey = normalizeNameKey(rawName);
                if (nameKey.isEmpty() || !rawName.matches(".*\\p{L}.*")) {
                    raw.problems.add(problem(rowNumber, rawName, null, "Sheet '" + sheetName + "', row " + rowNumber
                            + ": '" + rawName + "' is not a readable employee name."));
                    continue;
                }
                raw.names.putIfAbsent(nameKey, rawName);

                for (Map.Entry<Integer, LocalDate> entry : dateCols.entrySet()) {
                    int col = entry.getKey();
                    LocalDate date = entry.getValue();
                    MealImportParsing.CellValue v = MealImportParsing.readMealCell(row.getCell(col), formatter, standardPrice);
                    switch (v.kind()) {
                        case ATE -> raw.meals.add(new ParsedMealItem(rawName, date, MealStatus.ATE, v.amount(), sheetName, rowNumber));
                        case NO_MEAL -> raw.noMealCells++;
                        case INVALID -> raw.problems.add(problem(rowNumber, rawName, date.toString(),
                                "Sheet '" + sheetName + "', cell " + new CellReference(r, col).formatAsString()
                                        + " (" + headerText.get(col) + "): " + v.reason()));
                    }
                }
            }
            if (namelessWithValues > 0) {
                raw.warnings.add("Sheet '" + sheetName + "': " + namelessWithValues
                        + " row(s) with values but no employee name were ignored (for example a totals row).");
            }
        }
        return raw;
    }

    private static boolean isNameHeader(String key) {
        return key.contains("name") || key.equals("employee") || key.equals("employees")
                || key.equals("nom") || key.equals("amazina") || key.equals("staff");
    }

    /** Standard template rows: "Ate" rows become ATE meals on the chosen date; "Not Ate" rows record nothing. */
    private RawImport rawFromStandardRows(List<ExcelEmployeeRowDTO> rows, LocalDate date, BigDecimal standardPrice) {
        RawImport raw = new RawImport();
        for (ExcelEmployeeRowDTO r : rows) {
            if ("INVALID".equalsIgnoreCase(r.getStatus())) {
                raw.problems.add(r);
                continue;
            }
            if ("DUPLICATE".equalsIgnoreCase(r.getStatus())) {
                raw.skippedRows++;
                continue;
            }
            String name = r.getEmployeeName().trim().replaceAll("\\s+", " ");
            raw.names.putIfAbsent(normalizeNameKey(name), name);
            if ("Ate".equalsIgnoreCase(r.getMealStatus())) {
                MealImportParsing.CellValue v = ateAmount(r.getAmountUsed(), standardPrice);
                if (v.kind() == MealImportParsing.Kind.ATE) {
                    raw.meals.add(new ParsedMealItem(name, date, MealStatus.ATE, v.amount(), "Standard", r.getRowNumber()));
                } else {
                    raw.problems.add(problem(r.getRowNumber(), name, date.toString(), "Row " + r.getRowNumber() + ": " + v.reason()));
                }
            } else {
                raw.noMealCells++;
            }
        }
        if (raw.skippedRows > 0) {
            raw.warnings.add(raw.skippedRows + " row(s) were skipped because their telephone number already exists.");
        }
        return raw;
    }

    /** Amount for an explicit ATE row: blank/0 uses the standard price (existing rule); others must be valid. */
    private static MealImportParsing.CellValue ateAmount(BigDecimal amount, BigDecimal standardPrice) {
        if (amount == null || amount.signum() == 0) {
            return new MealImportParsing.CellValue(MealImportParsing.Kind.ATE, standardPrice, null);
        }
        return MealImportParsing.classifyAmount(amount, standardPrice, amount.stripTrailingZeros().toPlainString());
    }

    private static ExcelEmployeeRowDTO problem(int rowNumber, String name, String date, String reason) {
        return ExcelEmployeeRowDTO.builder()
                .rowNumber(rowNumber)
                .employeeName(name)
                .mealDate(date)
                .status("INVALID")
                .errorReason(reason)
                .build();
    }

    /* ======================================================================
       Planning: resolve employees, remove duplicates, compute exact totals
       ====================================================================== */

    private record PlannedMeal(String nameKey, String displayName, LocalDate date, BigDecimal amount) {
    }

    private static final class ImportPlan {
        final Map<String, Long> existingIds = new HashMap<>();          // name key -> employee id
        final Map<String, String> newEmployees = new LinkedHashMap<>(); // name key -> display name
        final Map<Long, String> codeById = new HashMap<>();
        final List<PlannedMeal> meals = new ArrayList<>();
        final List<ExcelEmployeeRowDTO> problems = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        int noMealCells;
        int repeatedCells;
        int skippedRows;

        BigDecimal totalAmount() {
            return meals.stream().map(PlannedMeal::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private ImportPlan buildPlan(RawImport raw) {
        ImportPlan plan = new ImportPlan();
        plan.problems.addAll(raw.problems);
        plan.warnings.addAll(raw.warnings);
        plan.noMealCells = raw.noMealCells;
        plan.skippedRows = raw.skippedRows;

        List<Employee> employees = employeeRepository.findAll();
        Map<String, Set<Long>> exact = new HashMap<>();
        Map<String, Set<Long>> firstNameOnly = new HashMap<>();
        Map<Long, Employee> byId = new HashMap<>();
        for (Employee e : employees) {
            byId.put(e.getId(), e);
            plan.codeById.put(e.getId(), e.getEmployeeCode());
            String first = e.getFirstName() == null ? "" : e.getFirstName();
            String last = e.getLastName() == null ? "" : e.getLastName();
            addKey(exact, first + " " + last, e.getId());
            addKey(exact, last + " " + first, e.getId());
            addKey(exact, e.getEmployeeCode(), e.getId());
            addKey(firstNameOnly, first, e.getId());
        }

        Set<String> ambiguous = new HashSet<>();
        for (Map.Entry<String, String> n : raw.names.entrySet()) {
            String key = n.getKey();
            Set<Long> ids = exact.getOrDefault(key, Set.of());
            if (ids.isEmpty()) {
                ids = firstNameOnly.getOrDefault(key, Set.of());
            }
            if (ids.size() > 1) {
                // Prefer the single ACTIVE employee if the others are inactive.
                Set<Long> active = new HashSet<>();
                for (Long id : ids) {
                    if (byId.get(id).getStatus() == EmployeeStatus.ACTIVE) active.add(id);
                }
                if (active.size() == 1) ids = active;
            }
            if (ids.size() == 1) {
                plan.existingIds.put(key, ids.iterator().next());
            } else if (ids.isEmpty()) {
                plan.newEmployees.put(key, n.getValue());
            } else {
                ambiguous.add(key);
                List<String> who = ids.stream().sorted().map(id -> {
                    Employee e = byId.get(id);
                    return e.getEmployeeCode() + " " + (e.getFirstName() + " " + e.getLastName()).trim();
                }).toList();
                plan.problems.add(problem(0, n.getValue(), null, "'" + n.getValue() + "' matches more than one employee ("
                        + String.join(", ", who) + "). Use the full name or the employee code in the file."));
            }
        }

        // One record per employee per day. Identical repeats are ignored; different values conflict.
        Map<String, PlannedMeal> unique = new LinkedHashMap<>();
        for (ParsedMealItem item : raw.meals) {
            String key = normalizeNameKey(item.employeeName);
            if (ambiguous.contains(key)) continue;
            String identity = plan.existingIds.containsKey(key) ? "id:" + plan.existingIds.get(key) : "new:" + key;
            String dk = identity + "#" + item.mealDate;
            PlannedMeal prev = unique.get(dk);
            if (prev == null) {
                unique.put(dk, new PlannedMeal(key, item.employeeName, item.mealDate, item.amount));
            } else if (prev.amount().compareTo(item.amount) == 0) {
                plan.repeatedCells++;
            } else {
                plan.problems.add(problem(item.rowNumber, item.employeeName, item.mealDate.toString(),
                        "'" + item.employeeName + "' appears more than once for " + item.mealDate + " with different amounts ("
                                + prev.amount().stripTrailingZeros().toPlainString() + " and "
                                + item.amount.stripTrailingZeros().toPlainString() + ")."));
            }
        }
        plan.meals.addAll(unique.values());
        if (plan.repeatedCells > 0) {
            plan.warnings.add(plan.repeatedCells + " repeated entr(ies) for the same employee and day were counted once.");
        }
        return plan;
    }

    private void addKey(Map<String, Set<Long>> index, String value, Long id) {
        String key = normalizeNameKey(value);
        if (!key.isEmpty()) {
            index.computeIfAbsent(key, k -> new HashSet<>()).add(id);
        }
    }

    /* ======================================================================
       Preview and save
       ====================================================================== */

    private ExcelImportPreviewResponse buildPreview(ImportPlan plan) {
        List<ExcelEmployeeRowDTO> rows = new ArrayList<>(plan.problems.subList(0, Math.min(500, plan.problems.size())));
        int seq = 1;
        for (PlannedMeal m : plan.meals) {
            if (seq > 100) break;
            Long id = plan.existingIds.get(m.nameKey());
            rows.add(ExcelEmployeeRowDTO.builder()
                    .rowNumber(seq++)
                    .employeeCode(id != null ? plan.codeById.get(id) : "NEW")
                    .employeeName(m.displayName())
                    .mealDate(m.date().toString())
                    .mealStatus("Ate")
                    .amountUsed(m.amount())
                    .status("VALID")
                    .build());
        }
        ExcelImportPreviewResponse preview = ExcelImportPreviewResponse.builder()
                .totalRows(plan.meals.size() + plan.problems.size())
                .validRows(plan.meals.size())
                .invalidRows(plan.problems.size())
                .duplicateRows(plan.repeatedCells + plan.skippedRows)
                .rows(rows)
                .build();
        applySummary(plan, preview);
        return preview;
    }

    private void applySummary(ImportPlan plan, ExcelImportPreviewResponse p) {
        p.setAteRecords(plan.meals.size());
        p.setNoMealCells(plan.noMealCells);
        p.setNewEmployees(plan.newEmployees.size());
        p.setTotalAmount(plan.totalAmount());
        p.setMonths(monthLines(plan.meals));
        p.setWarnings(plan.warnings);
        plan.meals.stream().map(PlannedMeal::date).min(Comparator.naturalOrder()).ifPresent(d -> p.setFirstMealDate(d.toString()));
        plan.meals.stream().map(PlannedMeal::date).max(Comparator.naturalOrder()).ifPresent(d -> p.setLastMealDate(d.toString()));
    }

    private static final java.time.format.DateTimeFormatter MONTH_LABEL =
            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /** "September 2026: 1,234 meals, 740,400 RWF" per month, from the planned records. */
    private List<String> monthLines(List<PlannedMeal> meals) {
        Map<java.time.YearMonth, long[]> counts = new TreeMap<>();
        Map<java.time.YearMonth, BigDecimal> sums = new TreeMap<>();
        for (PlannedMeal m : meals) {
            java.time.YearMonth ym = java.time.YearMonth.from(m.date());
            counts.computeIfAbsent(ym, k -> new long[1])[0]++;
            sums.merge(ym, m.amount(), BigDecimal::add);
        }
        List<String> lines = new ArrayList<>();
        for (java.time.YearMonth ym : counts.keySet()) {
            lines.add(ym.format(MONTH_LABEL) + ": " + String.format(Locale.ENGLISH, "%,d", counts.get(ym)[0])
                    + " meals, " + money(sums.get(ym)) + " RWF");
        }
        return lines;
    }

    private static String money(BigDecimal v) {
        return new java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v);
    }

    private ExcelImportResultResponse savePlan(ImportPlan plan, String recordedBy) {
        if (!plan.problems.isEmpty()) {
            StringBuilder msg = new StringBuilder("Nothing was imported: ").append(plan.problems.size())
                    .append(" problem(s) must be corrected in the file first. ");
            plan.problems.stream().limit(5).forEach(p -> msg.append(p.getErrorReason()).append(' '));
            if (plan.problems.size() > 5) msg.append("Use Preview to see the full list.");
            throw new BadRequestException(msg.toString().trim());
        }
        if (plan.meals.isEmpty()) {
            throw new BadRequestException("No meals were found in the file. Cells with an amount (for example 600) or 'ate' are imported as ATE; empty cells are not recorded.");
        }

        // 1. Create employees that are not on the list yet.
        Map<String, Long> idByKey = new HashMap<>(plan.existingIds);
        if (!plan.newEmployees.isEmpty()) {
            int nextCodeSeq = getNextEmployeeCodeSequence();
            Map<String, String> codeByKey = new LinkedHashMap<>();
            List<Object[]> inserts = new ArrayList<>();
            for (Map.Entry<String, String> e : plan.newEmployees.entrySet()) {
                String code = String.format("EMP%03d", nextCodeSeq++);
                codeByKey.put(e.getKey(), code);
                String fullName = e.getValue().trim();
                int space = fullName.indexOf(' ');
                String firstName = space > 0 ? fullName.substring(0, space).trim() : fullName;
                String lastName = space > 0 ? fullName.substring(space + 1).trim() : "";
                inserts.add(new Object[]{code, firstName, lastName, "General", "Worker", "", null});
            }
            jdbcTemplate.batchUpdate(
                    "INSERT INTO employees (employee_code, first_name, last_name, department, position, phone, email, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())", inserts);
            Map<String, Long> idByCode = new HashMap<>();
            jdbcTemplate.query("SELECT id, employee_code FROM employees WHERE employee_code IN ("
                            + String.join(",", Collections.nCopies(codeByKey.size(), "?")) + ")",
                    rs -> { idByCode.put(rs.getString(2), rs.getLong(1)); },
                    codeByKey.values().toArray());
            codeByKey.forEach((key, code) -> idByKey.put(key, idByCode.get(code)));
        }

        // 2. Count which (employee, day) rows already exist, to report created vs updated honestly.
        LocalDate first = plan.meals.stream().map(PlannedMeal::date).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate last = plan.meals.stream().map(PlannedMeal::date).max(Comparator.naturalOrder()).orElseThrow();
        Set<String> existingKeys = new HashSet<>();
        jdbcTemplate.query("SELECT employee_id, meal_date FROM meal_records WHERE meal_date BETWEEN ? AND ?",
                rs -> { existingKeys.add(rs.getLong(1) + "#" + rs.getDate(2).toLocalDate()); },
                java.sql.Date.valueOf(first), java.sql.Date.valueOf(last));

        // 3. Upsert: one row per employee per day, always ATE with the exact amount from the file.
        java.sql.Timestamp importedAt = java.sql.Timestamp.valueOf(BusinessTime.nowForDisplay().withNano(0));
        List<Object[]> params = new ArrayList<>(plan.meals.size());
        Set<Long> employeeIds = new HashSet<>();
        int updated = 0;
        for (PlannedMeal m : plan.meals) {
            Long empId = idByKey.get(m.nameKey());
            if (empId == null) {
                throw new IllegalStateException("Employee could not be resolved during import.");
            }
            employeeIds.add(empId);
            if (existingKeys.contains(empId + "#" + m.date())) updated++;
            params.add(new Object[]{empId, java.sql.Date.valueOf(m.date()), m.amount(), recordedBy, importedAt});
        }
        String upsertSql = "INSERT INTO meal_records (employee_id, meal_date, meal_status, amount, recorded_by, source, imported_at, created_at, updated_at) " +
                "VALUES (?, ?, 'ATE', ?, ?, 'IMPORT', ?, NOW(), NOW()) " +
                "ON CONFLICT (employee_id, meal_date) DO UPDATE SET " +
                "    meal_status = 'ATE', amount = EXCLUDED.amount, recorded_by = EXCLUDED.recorded_by, " +
                "    source = 'IMPORT', imported_at = EXCLUDED.imported_at, updated_at = NOW()";
        for (int i = 0; i < params.size(); i += 2000) {
            jdbcTemplate.batchUpdate(upsertSql, params.subList(i, Math.min(i + 2000, params.size())));
        }

        // 4. Verify that exactly these (employee, day) rows are now stored as ATE with exactly
        //    the file's total; otherwise roll everything back.
        BigDecimal expected = plan.totalAmount();
        Long[] empIds = new Long[params.size()];
        java.sql.Date[] days = new java.sql.Date[params.size()];
        for (int i = 0; i < params.size(); i++) {
            empIds[i] = (Long) params.get(i)[0];
            days[i] = (java.sql.Date) params.get(i)[1];
        }
        Object[] stored = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Object[]>) con -> {
            try (java.sql.PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*), COALESCE(SUM(m.amount), 0) FROM meal_records m " +
                    "JOIN unnest(?::bigint[], ?::date[]) AS t(emp, day) ON m.employee_id = t.emp AND m.meal_date = t.day " +
                    "WHERE m.meal_status = 'ATE' AND m.source = 'IMPORT' AND m.imported_at = ?")) {
                ps.setArray(1, con.createArrayOf("bigint", empIds));
                ps.setArray(2, con.createArrayOf("date", days));
                ps.setTimestamp(3, importedAt);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return new Object[]{rs.getLong(1), rs.getBigDecimal(2)};
                }
            }
        });
        long storedCount = (Long) stored[0];
        BigDecimal storedTotal = (BigDecimal) stored[1];
        if (storedCount != plan.meals.size() || storedTotal.compareTo(expected) != 0) {
            logger.error("Import verification failed: expected {} records / {}, stored {} / {}",
                    plan.meals.size(), expected, storedCount, storedTotal);
            throw new IllegalStateException("Import verification failed; no changes were saved.");
        }

        int created = plan.meals.size() - updated;
        List<String> months = monthLines(plan.meals);
        String range = first.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " to "
                + last.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String summaryMsg = String.format(Locale.ENGLISH,
                "Imported %,d ATE meal record(s) for %d employee(s), meal dates %s, total %s RWF (%,d new, %,d updated).",
                plan.meals.size(), employeeIds.size(), range, money(expected), created, updated);

        auditLogService.logActionSync("EXCEL_MEAL_IMPORT", "MEAL_RECORD", "BULK",
                summaryMsg + " " + String.join("; ", months)
                        + (plan.newEmployees.isEmpty() ? "" : " New employees created: " + plan.newEmployees.size() + "."));

        ExcelImportResultResponse result = ExcelImportResultResponse.builder()
                .importedCount(plan.meals.size())
                .employeesProcessed(employeeIds.size())
                .mealRecordsCreated(plan.meals.size())
                .ateCount(plan.meals.size())
                .didNotEatCount(0)
                .duplicateCount(plan.repeatedCells + plan.skippedRows)
                .invalidCount(0)
                .message(summaryMsg)
                .failedRows(Collections.emptyList())
                .build();
        result.setRecordsCreated(created);
        result.setRecordsUpdated(updated);
        result.setAteRecords(plan.meals.size());
        result.setNoMealCells(plan.noMealCells);
        result.setNewEmployees(plan.newEmployees.size());
        result.setTotalAmount(expected);
        result.setMonths(months);
        result.setWarnings(plan.warnings);
        result.setFirstMealDate(first.toString());
        result.setLastMealDate(last.toString());
        return result;
    }


    /**
     * Helper for standard single-date 5-column format.
     */
    private ExcelImportPreviewResponse parseStandardFormat(Sheet sheet, LocalDate targetDate, DataFormatter formatter) {
        BigDecimal standardPrice = settingsService.getStandardMealPrice();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BadRequestException("No header row found on row 1 of the Excel sheet.");
        }

        Map<String, Integer> colIndexMap = mapHeaders(headerRow);
        validateRequiredColumns(colIndexMap);

        int nameCol = colIndexMap.get("name");
        int phoneCol = colIndexMap.get("phone");
        int posCol = colIndexMap.get("position");
        int statusCol = colIndexMap.get("mealstatus");
        int amountCol = colIndexMap.get("amount");

        List<String> existingPhones = employeeRepository.findAllPhones();
        Set<String> existingPhoneSet = existingPhones.stream()
                .filter(Objects::nonNull)
                .map(this::normalizePhone)
                .collect(Collectors.toSet());

        Set<String> seenPhonesInFile = new HashSet<>();
        List<ExcelEmployeeRowDTO> previewRows = new ArrayList<>();

        int totalRows = 0;
        int validRows = 0;
        int invalidRows = 0;
        int duplicateRows = 0;

        int lastRowNum = sheet.getLastRowNum();
        for (int r = 1; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row, formatter)) {
                continue;
            }

            totalRows++;
            int displayRowNumber = r + 1;

            String empName = getCellString(row, nameCol, formatter);
            String phone = getCellString(row, phoneCol, formatter);
            String position = getCellString(row, posCol, formatter);
            String mealStatusRaw = getCellString(row, statusCol, formatter);
            String amountRaw = getCellString(row, amountCol, formatter);

            List<String> errors = new ArrayList<>();

            if (empName.isBlank()) {
                errors.add("Employee Name is required.");
            }

            String normalizedPhone = normalizePhone(phone);
            if (phone.isBlank()) {
                errors.add("Telephone is required.");
            } else if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
                errors.add("Invalid telephone format ('" + phone + "'). Must be 8-15 digits.");
            }

            if (position.isBlank()) {
                errors.add("Position is required.");
            }

            String parsedMealStatus = parseMealStatus(mealStatusRaw);
            if (parsedMealStatus == null) {
                errors.add("Invalid Meal Status ('" + mealStatusRaw + "'). Expected 'Ate' or 'Not Ate'.");
            }

            BigDecimal parsedAmount = BigDecimal.ZERO;
            if (parsedMealStatus != null) {
                if ("ATE".equalsIgnoreCase(parsedMealStatus)) {
                    if (amountRaw.isBlank()) {
                        parsedAmount = standardPrice;
                    } else {
                        try {
                            String cleanAmount = amountRaw.replaceAll("[^0-9.]", "");
                            parsedAmount = new BigDecimal(cleanAmount);
                            if (parsedAmount.compareTo(BigDecimal.ZERO) < 0) {
                                errors.add("Amount Used cannot be negative.");
                            }
                        } catch (Exception e) {
                            errors.add("Invalid numeric Amount Used ('" + amountRaw + "').");
                        }
                    }
                } else {
                    if (!amountRaw.isBlank()) {
                        try {
                            String cleanAmount = amountRaw.replaceAll("[^0-9.]", "");
                            parsedAmount = new BigDecimal(cleanAmount);
                            if (parsedAmount.compareTo(BigDecimal.ZERO) > 0) {
                                errors.add("Amount must be 0 for Not Ate / Did Not Eat status.");
                            }
                        } catch (Exception e) {
                            parsedAmount = BigDecimal.ZERO;
                        }
                    }
                }
            }

            boolean isDuplicate = false;
            String duplicateReason = null;

            if (errors.isEmpty() && !normalizedPhone.isBlank()) {
                if (existingPhoneSet.contains(normalizedPhone)) {
                    isDuplicate = true;
                    duplicateReason = "Duplicate: Telephone '" + phone + "' already exists in database.";
                } else if (seenPhonesInFile.contains(normalizedPhone)) {
                    isDuplicate = true;
                    duplicateReason = "Duplicate: Telephone '" + phone + "' appears multiple times in uploaded file.";
                } else {
                    seenPhonesInFile.add(normalizedPhone);
                }
            }

            String rowStatus;
            String errorReasonText = null;

            if (!errors.isEmpty()) {
                rowStatus = "INVALID";
                errorReasonText = String.join(" ", errors);
                invalidRows++;
            } else if (isDuplicate) {
                rowStatus = "DUPLICATE";
                errorReasonText = duplicateReason;
                duplicateRows++;
            } else {
                rowStatus = "VALID";
                validRows++;
            }

            previewRows.add(ExcelEmployeeRowDTO.builder()
                    .rowNumber(displayRowNumber)
                    .employeeName(empName)
                    .telephone(phone)
                    .position(position)
                    .mealDate(targetDate.toString())
                    .mealStatus(parsedMealStatus != null ? ("ATE".equalsIgnoreCase(parsedMealStatus) ? "Ate" : "Not Ate") : mealStatusRaw)
                    .amountUsed(parsedAmount)
                    .status(rowStatus)
                    .errorReason(errorReasonText)
                    .build());
        }

        if (totalRows == 0) {
            throw new BadRequestException("The Excel sheet contains no employee data rows.");
        }

        return ExcelImportPreviewResponse.builder()
                .totalRows(totalRows)
                .validRows(validRows)
                .invalidRows(invalidRows)
                .duplicateRows(duplicateRows)
                .rows(previewRows)
                .build();
    }

    private String normalizeNameKey(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an Excel file to upload.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx") && !originalFilename.toLowerCase().endsWith(".xls"))) {
            throw new BadRequestException("Invalid file format. Please upload an Excel file (.xlsx or .xls).");
        }
    }

    /**
     * Generates a downloadable Excel report detailing all skipped/invalid rows with failure reasons.
     */
    public byte[] generateErrorReport(List<ExcelEmployeeRowDTO> failedRows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import Error Report");

            // Styles
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle duplicateStyle = workbook.createCellStyle();
            duplicateStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            duplicateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle invalidStyle = workbook.createCellStyle();
            invalidStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            invalidStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Row #", "Employee Name", "Telephone", "Position", "Meal Status", "Amount Used", "Status", "Failure Reason"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (ExcelEmployeeRowDTO row : failedRows) {
                Row dataRow = sheet.createRow(r++);
                dataRow.createCell(0).setCellValue(row.getRowNumber());
                dataRow.createCell(1).setCellValue(row.getEmployeeName() != null ? row.getEmployeeName() : "");
                dataRow.createCell(2).setCellValue(row.getTelephone() != null ? row.getTelephone() : "");
                dataRow.createCell(3).setCellValue(row.getPosition() != null ? row.getPosition() : "");
                dataRow.createCell(4).setCellValue(row.getMealStatus() != null ? row.getMealStatus() : "");
                dataRow.createCell(5).setCellValue(row.getAmountUsed() != null ? row.getAmountUsed().toString() : "0");
                dataRow.createCell(6).setCellValue(row.getStatus() != null ? row.getStatus() : "");
                dataRow.createCell(7).setCellValue(row.getErrorReason() != null ? row.getErrorReason() : "");

                CellStyle rowStyle = "DUPLICATE".equalsIgnoreCase(row.getStatus()) ? duplicateStyle : invalidStyle;
                for (int c = 0; c < headers.length; c++) {
                    dataRow.getCell(c).setCellStyle(rowStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3600));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate error report: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> mapHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null) {
                String text = formatter.formatCellValue(cell).trim().toLowerCase().replaceAll("[^a-z]", "");
                if (text.contains("name") || text.equals("employee")) {
                    map.put("name", c);
                } else if (text.contains("phone") || text.contains("telephone") || text.contains("tel") || text.contains("mobile")) {
                    map.put("phone", c);
                } else if (text.contains("position") || text.contains("role") || text.contains("job") || text.contains("title")) {
                    map.put("position", c);
                } else if (text.contains("meal") || text.contains("status")) {
                    map.put("mealstatus", c);
                } else if (text.contains("amount") || text.contains("cost") || text.contains("price")) {
                    map.put("amount", c);
                }
            }
        }
        return map;
    }

    private void validateRequiredColumns(Map<String, Integer> colIndexMap) {
        if (!colIndexMap.containsKey("name")) {
            throw new BadRequestException("Missing required column: Employee Name");
        }
        if (!colIndexMap.containsKey("phone")) {
            throw new BadRequestException("Missing required column: Telephone");
        }
        if (!colIndexMap.containsKey("position")) {
            throw new BadRequestException("Missing required column: Position");
        }
        if (!colIndexMap.containsKey("mealstatus")) {
            throw new BadRequestException("Missing required column: Meal Status");
        }
        if (!colIndexMap.containsKey("amount")) {
            throw new BadRequestException("Missing required column: Amount Used");
        }
    }

    private String getCellString(Row row, int colIndex, DataFormatter formatter) {
        if (colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String clean = phone.replaceAll("[\\s-]", "");
        if (clean.startsWith("+250")) {
            clean = "0" + clean.substring(4);
        } else if (clean.startsWith("250") && clean.length() == 12) {
            clean = "0" + clean.substring(3);
        } else if (clean.length() == 9 && (clean.startsWith("7") || clean.startsWith("8") || clean.startsWith("9") || clean.startsWith("2") || clean.startsWith("3"))) {
            clean = "0" + clean;
        }
        return clean;
    }

    private String parseMealStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim().toLowerCase().replaceAll("[_\\s-]", "");
        if (clean.equals("ate") || clean.equals("yes") || clean.equals("true") || clean.equals("1")) {
            return "ATE";
        }
        if (clean.equals("notate") || clean.equals("didnoteat") || clean.equals("no") || clean.equals("false") || clean.equals("0")) {
            return "DID_NOT_EAT";
        }
        return null;
    }

    private int getNextEmployeeCodeSequence() {
        List<String> codes = employeeRepository.findAllEmployeeCodes();
        int maxSeq = 0;
        for (String code : codes) {
            if (code != null) {
                Matcher m = EMP_CODE_PATTERN.matcher(code.trim());
                if (m.find()) {
                    try {
                        int val = Integer.parseInt(m.group(1));
                        if (val > maxSeq) {
                            maxSeq = val;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return maxSeq + 1;
    }
}
