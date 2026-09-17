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
     * Reads Excel, validates column headers, detects Matrix (multi-date) or Standard format,
     * validates data, and returns a comprehensive preview.
     */
    @Transactional(readOnly = true)
    public ExcelImportPreviewResponse previewExcel(MultipartFile file, LocalDate defaultDate) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an Excel file to upload.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx") && !originalFilename.toLowerCase().endsWith(".xls"))) {
            throw new BadRequestException("Invalid file format. Please upload an Excel file (.xlsx or .xls).");
        }

        LocalDate targetDate = (defaultDate != null) ? defaultDate : LocalDate.now();
        int targetYear = targetDate.getYear();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("The uploaded Excel workbook contains no sheets.");
            }

            // Pick active or first sheet
            Sheet sheet = workbook.getSheetAt(0);
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet curSheet = workbook.getSheetAt(s);
                if (curSheet.getPhysicalNumberOfRows() > 0) {
                    sheet = curSheet;
                    break;
                }
            }

            if (sheet.getPhysicalNumberOfRows() < 1) {
                throw new BadRequestException("The Excel sheet is empty.");
            }

            String sheetName = sheet.getSheetName();
            DataFormatter formatter = new DataFormatter();

            // Check if sheet is Matrix (Multi-Date) format or Standard (Single-Date) format
            // Scan top 4 rows for header detection
            int nameCol = -1;
            int headerRowIndex = -1;
            Map<Integer, LocalDate> dateColsMap = new TreeMap<>();
            boolean isMatrixFormat = false;

            for (int r = 0; r < Math.min(5, sheet.getPhysicalNumberOfRows() + 2); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<Integer, LocalDate> potentialDates = new TreeMap<>();
                int foundNameCol = -1;

                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    String val = formatter.formatCellValue(cell).trim();
                    if (val.isEmpty()) continue;

                    String valLower = val.toLowerCase().replaceAll("[^a-z]", "");
                    if (valLower.contains("name") || valLower.equals("employee") || valLower.equals("nom") || valLower.equals("amazina")) {
                        foundNameCol = c;
                    } else if (!valLower.contains("total") && !valLower.contains("amountpaid")) {
                        LocalDate parsedDate = parseDateHeader(cell, val, targetYear, sheetName);
                        if (parsedDate != null) {
                            potentialDates.put(c, parsedDate);
                        }
                    }
                }

                if (!potentialDates.isEmpty()) {
                    isMatrixFormat = true;
                    dateColsMap = potentialDates;
                    headerRowIndex = r;
                    nameCol = (foundNameCol >= 0) ? foundNameCol : 0;
                    break;
                }
            }

            // If matrix format detected:
            if (isMatrixFormat && !dateColsMap.isEmpty()) {
                return parseMatrixFormat(sheet, headerRowIndex, nameCol, dateColsMap, formatter);
            }

            // Otherwise, fallback to standard single-date format
            return parseStandardFormat(sheet, targetDate, formatter);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse Excel file", e);
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    /**
     * Parses the real multi-date matrix Excel format (Names in Col A, multiple Date columns).
     * Amount > 0 -> ATE
     * Empty / 0 -> DID_NOT_EAT
     */
    private ExcelImportPreviewResponse parseMatrixFormat(Sheet sheet,
                                                          int headerRowIndex,
                                                          int nameCol,
                                                          Map<Integer, LocalDate> dateColsMap,
                                                          DataFormatter formatter) {
        // Fetch all existing employees for fast name-based matching
        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, Employee> employeeLookup = new HashMap<>();
        for (Employee emp : allEmployees) {
            String fullName = (emp.getFirstName() + " " + emp.getLastName()).trim();
            employeeLookup.put(normalizeNameKey(fullName), emp);
            String reverseName = (emp.getLastName() + " " + emp.getFirstName()).trim();
            employeeLookup.put(normalizeNameKey(reverseName), emp);
            if (!emp.getFirstName().isBlank()) {
                employeeLookup.put(normalizeNameKey(emp.getFirstName()), emp);
            }
            if (emp.getEmployeeCode() != null) {
                employeeLookup.put(normalizeNameKey(emp.getEmployeeCode()), emp);
            }
        }

        List<ExcelEmployeeRowDTO> previewRows = new ArrayList<>();
        int totalProcessedRows = 0;
        int validRows = 0;
        int invalidRows = 0;
        int duplicateRows = 0;

        int lastRowNum = sheet.getLastRowNum();
        int displaySeq = 1;

        for (int r = headerRowIndex + 1; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row, formatter)) {
                continue;
            }

            String rawName = getCellString(row, nameCol, formatter);
            if (rawName.isBlank()) {
                continue;
            }

            String cleanLower = rawName.trim().toLowerCase();
            if (cleanLower.startsWith("total") || cleanLower.startsWith("summary")) {
                continue; // Skip total/summary row
            }

            totalProcessedRows++;

            // Lookup existing employee in DB
            Employee matchedEmp = employeeLookup.get(normalizeNameKey(rawName));
            String empCode = matchedEmp != null ? matchedEmp.getEmployeeCode() : null;
            String position = matchedEmp != null ? matchedEmp.getPosition() : "Worker";
            String phone = matchedEmp != null ? matchedEmp.getPhone() : "";

            // For each date column, evaluate cell
            for (Map.Entry<Integer, LocalDate> entry : dateColsMap.entrySet()) {
                int colIdx = entry.getKey();
                LocalDate date = entry.getValue();

                Cell cell = row.getCell(colIdx);
                BigDecimal amount = BigDecimal.ZERO;
                String mealStatusStr = "DID_NOT_EAT";

                if (cell != null) {
                    if (cell.getCellType() == CellType.NUMERIC) {
                        double val = cell.getNumericCellValue();
                        if (val > 0) {
                            amount = BigDecimal.valueOf(val);
                            mealStatusStr = "ATE";
                        }
                    } else if (cell.getCellType() == CellType.STRING) {
                        String strVal = formatter.formatCellValue(cell).trim().replaceAll("[^0-9.]", "");
                        if (!strVal.isEmpty()) {
                            try {
                                double val = Double.parseDouble(strVal);
                                if (val > 0) {
                                    amount = BigDecimal.valueOf(val);
                                    mealStatusStr = "ATE";
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // If empty cell -> DID_NOT_EAT, amount = 0
                // If amount > 0 -> ATE, amount = parsed amount
                String status = "VALID";
                String errorReason = null;

                if (rawName.isBlank()) {
                    status = "INVALID";
                    errorReason = "Employee Name is missing.";
                    invalidRows++;
                } else {
                    validRows++;
                }

                previewRows.add(ExcelEmployeeRowDTO.builder()
                        .rowNumber(displaySeq++)
                        .employeeCode(empCode)
                        .employeeName(rawName.trim())
                        .telephone(phone)
                        .position(position)
                        .mealDate(date.toString())
                        .mealStatus("ATE".equalsIgnoreCase(mealStatusStr) ? "Ate" : "Not Ate")
                        .amountUsed(amount)
                        .status(status)
                        .errorReason(errorReason)
                        .build());
            }
        }

        if (previewRows.isEmpty()) {
            throw new BadRequestException("No employee meal records could be extracted from the sheet.");
        }

        return ExcelImportPreviewResponse.builder()
                .totalRows(previewRows.size())
                .validRows(validRows)
                .invalidRows(invalidRows)
                .duplicateRows(duplicateRows)
                .rows(previewRows)
                .build();
    }

    /**
     * Fallback for standard single-date 5-column format.
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

    /**
     * Bulk inserts/upserts employee records and creates corresponding meal records across dates in a single transaction.
     */
    @Transactional
    public ExcelImportResultResponse confirmImport(ExcelImportConfirmRequest request) {
        if (request == null || request.getRows() == null || request.getRows().isEmpty()) {
            throw new BadRequestException("No employee rows provided for import.");
        }

        LocalDate fallbackDate = (request.getMealDate() != null) ? request.getMealDate() : LocalDate.now();

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
                    .employeesProcessed(0)
                    .mealRecordsCreated(0)
                    .ateCount(0)
                    .didNotEatCount(0)
                    .duplicateCount(duplicateCount)
                    .invalidCount(invalidCount)
                    .message("No valid rows to import.")
                    .failedRows(failedRows)
                    .build();
        }

        // 1. Fetch all existing employees & build fast lookup
        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, Employee> employeeLookup = new HashMap<>();
        for (Employee emp : allEmployees) {
            String fullName = (emp.getFirstName() + " " + emp.getLastName()).trim();
            employeeLookup.put(normalizeNameKey(fullName), emp);
            String reverseName = (emp.getLastName() + " " + emp.getFirstName()).trim();
            employeeLookup.put(normalizeNameKey(reverseName), emp);
            if (!emp.getFirstName().isBlank()) {
                employeeLookup.put(normalizeNameKey(emp.getFirstName()), emp);
            }
            if (emp.getEmployeeCode() != null) {
                employeeLookup.put(normalizeNameKey(emp.getEmployeeCode()), emp);
            }
        }

        // 2. Identify and create any missing employees
        int nextCodeSeq = getNextEmployeeCodeSequence();
        List<Employee> newEmployeesToSave = new ArrayList<>();
        Set<String> processedNewNames = new HashSet<>();

        for (ExcelEmployeeRowDTO row : validRows) {
            String nameKey = normalizeNameKey(row.getEmployeeName());
            if (!employeeLookup.containsKey(nameKey) && !processedNewNames.contains(nameKey)) {
                processedNewNames.add(nameKey);

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

                String phone = (row.getTelephone() != null && !row.getTelephone().isBlank())
                        ? normalizePhone(row.getTelephone())
                        : "078" + String.format("%07d", (int)(Math.random() * 10000000));

                String position = (row.getPosition() != null && !row.getPosition().isBlank())
                        ? row.getPosition().trim()
                        : "Worker";

                Employee newEmp = Employee.builder()
                        .employeeCode(employeeCode)
                        .firstName(firstName)
                        .lastName(lastName)
                        .department("General")
                        .position(position)
                        .phone(phone)
                        .status(EmployeeStatus.ACTIVE)
                        .build();

                newEmployeesToSave.add(newEmp);
            }
        }

        if (!newEmployeesToSave.isEmpty()) {
            List<Employee> savedNew = employeeRepository.saveAll(newEmployeesToSave);
            for (Employee emp : savedNew) {
                String fullName = (emp.getFirstName() + " " + emp.getLastName()).trim();
                employeeLookup.put(normalizeNameKey(fullName), emp);
                String reverseName = (emp.getLastName() + " " + emp.getFirstName()).trim();
                employeeLookup.put(normalizeNameKey(reverseName), emp);
                if (!emp.getFirstName().isBlank()) {
                    employeeLookup.put(normalizeNameKey(emp.getFirstName()), emp);
                }
            }
        }

        // 3. Process Meal Records for each row (upsert if exists for employee + date)
        int ateCount = 0;
        int didNotEatCount = 0;
        Set<Long> uniqueEmployeesProcessed = new HashSet<>();
        List<MealRecord> mealRecordsToSave = new ArrayList<>();

        for (ExcelEmployeeRowDTO row : validRows) {
            Employee emp = employeeLookup.get(normalizeNameKey(row.getEmployeeName()));
            if (emp == null) continue;

            uniqueEmployeesProcessed.add(emp.getId());

            LocalDate recordDate = fallbackDate;
            if (row.getMealDate() != null && !row.getMealDate().isBlank()) {
                try {
                    recordDate = LocalDate.parse(row.getMealDate().trim());
                } catch (Exception ignored) {}
            }

            boolean ate = "Ate".equalsIgnoreCase(row.getMealStatus()) || "ATE".equalsIgnoreCase(row.getMealStatus());
            MealStatus mStatus = ate ? MealStatus.ATE : MealStatus.DID_NOT_EAT;
            BigDecimal amount = ate ? (row.getAmountUsed() != null ? row.getAmountUsed() : new BigDecimal("600.00")) : BigDecimal.ZERO;

            if (ate) {
                ateCount++;
            } else {
                didNotEatCount++;
            }

            // Check if existing record exists for this employee + date
            Optional<MealRecord> existingOpt = mealRecordRepository.findByEmployeeIdAndMealDate(emp.getId(), recordDate);
            if (existingOpt.isPresent()) {
                MealRecord existing = existingOpt.get();
                existing.setMealStatus(mStatus);
                existing.setAmount(amount);
                existing.setRecordedBy(recordedBy);
                mealRecordsToSave.add(existing);
            } else {
                MealRecord mr = MealRecord.builder()
                        .employee(emp)
                        .mealDate(recordDate)
                        .mealStatus(mStatus)
                        .amount(amount)
                        .recordedBy(recordedBy)
                        .build();
                mealRecordsToSave.add(mr);
            }
        }

        mealRecordRepository.saveAll(mealRecordsToSave);

        int employeesCount = uniqueEmployeesProcessed.size();
        int recordsCount = mealRecordsToSave.size();

        auditLogService.logAction("EXCEL_EMPLOYEE_IMPORT", "EMPLOYEE", "BULK",
                "Successfully imported " + recordsCount + " meal records for " + employeesCount + " employees from Excel");

        String summaryMsg = String.format("Import Completed: %d employee(s) processed, %d meal record(s) created/updated (%d ATE, %d DID NOT EAT).",
                employeesCount, recordsCount, ateCount, didNotEatCount);

        return ExcelImportResultResponse.builder()
                .importedCount(recordsCount)
                .employeesProcessed(employeesCount)
                .mealRecordsCreated(recordsCount)
                .ateCount(ateCount)
                .didNotEatCount(didNotEatCount)
                .duplicateCount(duplicateCount)
                .invalidCount(invalidCount)
                .message(summaryMsg)
                .failedRows(failedRows)
                .build();
    }

    /**
     * Helper to parse date headers from various Excel string/numeric formats.
     */
    private LocalDate parseDateHeader(Cell cell, String cellText, int defaultYear, String sheetName) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cellText == null || cellText.isBlank()) return null;
        String text = cellText.trim().toLowerCase();

        // Check full ISO YYYY-MM-DD
        if (text.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            try {
                return LocalDate.parse(text);
            } catch (Exception ignored) {}
        }

        // Check DD/MM/YYYY or DD-MM-YYYY
        Pattern fullDatePattern = Pattern.compile("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})$");
        Matcher fdm = fullDatePattern.matcher(text);
        if (fdm.find()) {
            int d = Integer.parseInt(fdm.group(1));
            int m = Integer.parseInt(fdm.group(2));
            int y = Integer.parseInt(fdm.group(3));
            if (y < 100) y += 2000;
            try {
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {}
        }

        // Check "22nd aug", "23rd aug", "24th aug", "22 aug", "22nd"
        Pattern dayMonthPattern = Pattern.compile("(\\d{1,2})(?:st|nd|rd|th)?(?:[\\s-_/]+([a-z]+))?");
        Matcher m = dayMonthPattern.matcher(text);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            String monthStr = m.group(2);
            int month = 0;
            if (monthStr != null && !monthStr.isBlank()) {
                month = parseMonth(monthStr);
            } else if (sheetName != null && !sheetName.isBlank()) {
                month = parseMonth(sheetName);
            }
            if (month == 0) {
                month = LocalDate.now().getMonthValue();
            }
            int year = defaultYear > 0 ? defaultYear : LocalDate.now().getYear();
            try {
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {}
        }

        // Check "aug 22", "august 22nd"
        Pattern monthDayPattern = Pattern.compile("([a-z]+)[\\s-_/]+(\\d{1,2})(?:st|nd|rd|th)?");
        Matcher m2 = monthDayPattern.matcher(text);
        if (m2.find()) {
            int month = parseMonth(m2.group(1));
            int day = Integer.parseInt(m2.group(2));
            if (month > 0) {
                int year = defaultYear > 0 ? defaultYear : LocalDate.now().getYear();
                try {
                    return LocalDate.of(year, month, day);
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private int parseMonth(String str) {
        if (str == null) return 0;
        String s = str.trim().toLowerCase();
        if (s.startsWith("jan")) return 1;
        if (s.startsWith("feb")) return 2;
        if (s.startsWith("mar")) return 3;
        if (s.startsWith("apr")) return 4;
        if (s.startsWith("may")) return 5;
        if (s.startsWith("jun")) return 6;
        if (s.startsWith("jul")) return 7;
        if (s.startsWith("aug")) return 8;
        if (s.startsWith("sep")) return 9;
        if (s.startsWith("oct")) return 10;
        if (s.startsWith("nov")) return 11;
        if (s.startsWith("dec")) return 12;
        return 0;
    }

    private String normalizeNameKey(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
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
