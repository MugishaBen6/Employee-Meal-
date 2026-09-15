package com.emeal.service;

import com.emeal.dto.excel.*;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s-]{8,15}$");
    private static final Pattern EMP_CODE_PATTERN = Pattern.compile("EMP(\\d+)", Pattern.CASE_INSENSITIVE);

    public ExcelImportService(EmployeeRepository employeeRepository,
                              MealRecordRepository mealRecordRepository,
                              AuditLogService auditLogService,
                              SettingsService settingsService) {
        this.employeeRepository = employeeRepository;
        this.mealRecordRepository = mealRecordRepository;
        this.auditLogService = auditLogService;
        this.settingsService = settingsService;
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
                {"John Doe", "0781234567", "Worker", "Ate", "1500"},
                {"Jane Smith", "0791234567", "Manager", "Not Ate", "0"},
                {"Eric Mugisha", "0787654321", "Driver", "Ate", "1500"}
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

    /**
     * Reads Excel, validates column headers, validates row data, detects duplicates, and returns preview.
     */
    @Transactional(readOnly = true)
    public ExcelImportPreviewResponse previewExcel(MultipartFile file, LocalDate mealDate) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an Excel file to upload.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx") && !originalFilename.toLowerCase().endsWith(".xls"))) {
            throw new BadRequestException("Invalid file format. Please upload an Excel file (.xlsx or .xls).");
        }

        BigDecimal standardPrice = settingsService.getSettingBigDecimal("STANDARD_MEAL_PRICE", new BigDecimal("1500.00"));

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("The uploaded Excel workbook contains no sheets.");
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 1) {
                throw new BadRequestException("The Excel sheet is empty. Please include the required column headers.");
            }

            // Find Header Row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("No header row found on row 1 of the Excel sheet.");
            }

            Map<String, Integer> colIndexMap = mapHeaders(headerRow);

            // Validate Required Columns
            validateRequiredColumns(colIndexMap);

            int nameCol = colIndexMap.get("name");
            int phoneCol = colIndexMap.get("phone");
            int posCol = colIndexMap.get("position");
            int statusCol = colIndexMap.get("mealstatus");
            int amountCol = colIndexMap.get("amount");

            // Fetch existing phones from DB for fast duplicate detection
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

            DataFormatter formatter = new DataFormatter();

            int lastRowNum = sheet.getLastRowNum();
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue; // Skip blank rows
                }

                totalRows++;
                int displayRowNumber = r + 1;

                String empName = getCellString(row, nameCol, formatter);
                String phone = getCellString(row, phoneCol, formatter);
                String position = getCellString(row, posCol, formatter);
                String mealStatusRaw = getCellString(row, statusCol, formatter);
                String amountRaw = getCellString(row, amountCol, formatter);

                List<String> errors = new ArrayList<>();

                // Validate Name
                if (empName.isBlank()) {
                    errors.add("Employee Name is required.");
                }

                // Validate Phone
                String normalizedPhone = normalizePhone(phone);
                if (phone.isBlank()) {
                    errors.add("Telephone is required.");
                } else if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
                    errors.add("Invalid telephone format ('" + phone + "'). Must be 8-15 digits.");
                }

                // Validate Position
                if (position.isBlank()) {
                    errors.add("Position is required.");
                }

                // Parse & Validate Meal Status
                String parsedMealStatus = parseMealStatus(mealStatusRaw);
                if (parsedMealStatus == null) {
                    errors.add("Invalid Meal Status ('" + mealStatusRaw + "'). Expected 'Ate' or 'Not Ate'.");
                }

                // Parse & Validate Amount
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
                    } else { // DID_NOT_EAT
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

                // Duplicate Checking
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

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse Excel file", e);
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    /**
     * Bulk inserts valid employee records and creates corresponding meal records in a single transaction.
     */
    @Transactional
    public ExcelImportResultResponse confirmImport(ExcelImportConfirmRequest request) {
        if (request == null || request.getRows() == null || request.getRows().isEmpty()) {
            throw new BadRequestException("No employee rows provided for import.");
        }

        LocalDate targetDate = (request.getMealDate() != null) ? request.getMealDate() : LocalDate.now();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String recordedBy = (auth != null && auth.getName() != null) ? auth.getName() : "ADMIN";

        // Filter valid rows only
        List<ExcelEmployeeRowDTO> validRows = request.getRows().stream()
                .filter(r -> "VALID".equalsIgnoreCase(r.getStatus()))
                .toList();

        List<ExcelEmployeeRowDTO> failedRows = request.getRows().stream()
                .filter(r -> !"VALID".equalsIgnoreCase(r.getStatus()))
                .toList();

        int duplicateCount = (int) request.getRows().stream().filter(r -> "DUPLICATE".equalsIgnoreCase(r.getStatus())).count();
        int invalidCount = (int) request.getRows().stream().filter(r -> "INVALID".equalsIgnoreCase(r.getStatus())).count();

        if (validRows.isEmpty()) {
            return ExcelImportResultResponse.builder()
                    .importedCount(0)
                    .duplicateCount(duplicateCount)
                    .invalidCount(invalidCount)
                    .failedRows(failedRows)
                    .build();
        }

        // Calculate next sequential employee code number
        int nextCodeSeq = getNextEmployeeCodeSequence();

        List<Employee> employeesToSave = new ArrayList<>();
        List<MealRecord> mealRecordsToSave = new ArrayList<>();

        for (ExcelEmployeeRowDTO row : validRows) {
            String employeeCode = String.format("EMP%03d", nextCodeSeq++);
            while (employeeRepository.existsByEmployeeCode(employeeCode)) {
                employeeCode = String.format("EMP%03d", nextCodeSeq++);
            }

            String fullName = row.getEmployeeName().trim();
            String firstName;
            String lastName;
            int spaceIdx = fullName.indexOf(" ");
            if (spaceIdx > 0) {
                firstName = fullName.substring(0, spaceIdx).trim();
                lastName = fullName.substring(spaceIdx + 1).trim();
            } else {
                firstName = fullName;
                lastName = "";
            }

            Employee employee = Employee.builder()
                    .employeeCode(employeeCode)
                    .firstName(firstName)
                    .lastName(lastName)
                    .department("General")
                    .position(row.getPosition().trim())
                    .phone(normalizePhone(row.getTelephone()))
                    .status(EmployeeStatus.ACTIVE)
                    .build();

            employeesToSave.add(employee);
        }

        // Batch save employees
        List<Employee> savedEmployees = employeeRepository.saveAll(employeesToSave);

        // Batch create meal records
        for (int i = 0; i < savedEmployees.size(); i++) {
            Employee emp = savedEmployees.get(i);
            ExcelEmployeeRowDTO row = validRows.get(i);

            boolean ate = "Ate".equalsIgnoreCase(row.getMealStatus()) || "ATE".equalsIgnoreCase(row.getMealStatus());
            MealStatus mStatus = ate ? MealStatus.ATE : MealStatus.DID_NOT_EAT;
            BigDecimal amount = ate ? (row.getAmountUsed() != null ? row.getAmountUsed() : new BigDecimal("1500.00")) : BigDecimal.ZERO;

            MealRecord mr = MealRecord.builder()
                    .employee(emp)
                    .mealDate(targetDate)
                    .mealStatus(mStatus)
                    .amount(amount)
                    .recordedBy(recordedBy)
                    .build();

            mealRecordsToSave.add(mr);
        }

        mealRecordRepository.saveAll(mealRecordsToSave);

        auditLogService.logAction("EXCEL_EMPLOYEE_IMPORT", "EMPLOYEE", "BULK",
                "Successfully imported " + savedEmployees.size() + " employees from Excel for date " + targetDate);

        return ExcelImportResultResponse.builder()
                .importedCount(savedEmployees.size())
                .duplicateCount(duplicateCount)
                .invalidCount(invalidCount)
                .failedRows(failedRows)
                .build();
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
        return phone.replaceAll("[\\s-]", "");
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
