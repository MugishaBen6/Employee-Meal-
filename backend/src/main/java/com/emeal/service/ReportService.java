package com.emeal.service;

import com.emeal.config.BusinessTime;

import com.emeal.dto.response.DailyReportSummaryDTO;
import com.emeal.dto.response.MealRecordDTO;
import com.emeal.entity.Employee;
import com.emeal.entity.EmployeeStatus;
import com.emeal.entity.MealRecord;
import com.emeal.entity.MealStatus;
import com.emeal.exception.BadRequestException;
import com.emeal.repository.EmployeeRepository;
import com.emeal.repository.MealRecordRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final MealRecordRepository mealRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SettingsService settingsService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReportService(MealRecordRepository mealRecordRepository, EmployeeRepository employeeRepository, SettingsService settingsService) {
        this.mealRecordRepository = mealRecordRepository;
        this.employeeRepository = employeeRepository;
        this.settingsService = settingsService;
    }

    /** Longest allowed report period, in days (inclusive), to keep exports bounded. */
    public static final int MAX_PERIOD_DAYS = 366;

    /** Inclusive report period. A single-day report has start == end. */
    public record ReportPeriod(LocalDate start, LocalDate end) {
        public boolean singleDay() {
            return start.equals(end);
        }
    }

    /**
     * Resolves the requested period.
     * <ul>
     *   <li>startDate / endDate (From / To) take precedence; if only one is given, it is used for both.</li>
     *   <li>Otherwise the legacy single {@code date} is used for both.</li>
     *   <li>With no dates at all, today's business date (Africa/Kigali) is used.</li>
     * </ul>
     * Rejects From after To, and periods longer than {@link #MAX_PERIOD_DAYS} days.
     */
    public ReportPeriod resolvePeriod(LocalDate date, LocalDate startDate, LocalDate endDate) {
        LocalDate start;
        LocalDate end;
        if (startDate != null || endDate != null) {
            start = (startDate != null) ? startDate : endDate;
            end = (endDate != null) ? endDate : startDate;
        } else if (date != null) {
            start = date;
            end = date;
        } else {
            start = BusinessTime.today();
            end = start;
        }
        if (start.isAfter(end)) {
            throw new BadRequestException("From Date must be on or before To Date.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_PERIOD_DAYS) {
            throw new BadRequestException("The report period cannot be longer than " + MAX_PERIOD_DAYS + " days.");
        }
        return new ReportPeriod(start, end);
    }

    /** "Daily_Meal_Report_2026-09-01" for one day, "Meal_Report_2026-09-01_to_2026-09-30" for a range. */
    public String reportFileBaseName(ReportPeriod period) {
        return period.singleDay()
                ? "Daily_Meal_Report_" + period.start()
                : "Meal_Report_" + period.start() + "_to_" + period.end();
    }

    /** Single-day report (kept for existing callers). */
    @Transactional(readOnly = true)
    public DailyReportSummaryDTO generateDailyReportSummary(LocalDate date, String department) {
        return generateReportSummary(resolvePeriod(date, null, null), department);
    }

    /**
     * Report for the inclusive period: only meal records whose meal date is between
     * period.start and period.end (both included). Excel and PDF exports use this same method.
     */
    @Transactional(readOnly = true)
    public DailyReportSummaryDTO generateReportSummary(ReportPeriod period, String department) {
        String companyName = settingsService.getSettingValue("COMPANY_NAME", "RWANDA PLASTIC INDUSTRY");
        String currency = settingsService.getSettingValue("CURRENCY", "RWF");

        long totalEmployees;
        if (department != null && !department.isBlank()) {
            totalEmployees = employeeRepository.countByStatusAndDepartment(EmployeeStatus.ACTIVE, department.trim());
        } else {
            totalEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        }

        List<MealRecord> mealRecords;
        if (department != null && !department.isBlank()) {
            mealRecords = mealRecordRepository.findByMealDateBetweenAndEmployeeDepartmentOrderByMealDateAscEmployeeEmployeeCodeAsc(
                    period.start(), period.end(), department.trim());
        } else {
            mealRecords = mealRecordRepository.findByMealDateBetweenOrderByMealDateAscEmployeeEmployeeCodeAsc(
                    period.start(), period.end());
        }

        List<MealRecordDTO> dtos = new ArrayList<>();
        long ateCount = 0;
        long didNotEatCount = 0;
        BigDecimal totalExpenditure = BigDecimal.ZERO;

        for (MealRecord mr : mealRecords) {
            if (mr.getMealStatus() == MealStatus.ATE) {
                ateCount++;
                totalExpenditure = totalExpenditure.add(mr.getAmount());
            } else if (mr.getMealStatus() == MealStatus.DID_NOT_EAT) {
                didNotEatCount++;
            }
            dtos.add(MealRecordDTO.fromEntity(mr));
        }

        BigDecimal averageMealCost = (ateCount > 0)
                ? totalExpenditure.divide(BigDecimal.valueOf(ateCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String formattedStart = period.start().format(DATE_FORMATTER);
        String formattedEnd = period.end().format(DATE_FORMATTER);

        DailyReportSummaryDTO summary = DailyReportSummaryDTO.builder()
                .reportDate(period.start())
                .formattedReportDate(period.singleDay() ? formattedStart : formattedStart + " - " + formattedEnd)
                .companyName(companyName)
                .totalEmployees(totalEmployees)
                .ateCount(ateCount)
                .didNotEatCount(didNotEatCount)
                .totalExpenditure(totalExpenditure)
                .averageMealCost(averageMealCost)
                .currency(currency)
                .records(dtos)
                .build();
        summary.setStartDate(period.start());
        summary.setEndDate(period.end());
        summary.setFormattedStartDate(formattedStart);
        summary.setFormattedEndDate(formattedEnd);
        summary.setSingleDay(period.singleDay());
        return summary;
    }

    /** Excel export with exactly the records of {@link #generateReportSummary} for the same period. */
    @Transactional(readOnly = true)
    public byte[] generateExcelReport(ReportPeriod period, String department) throws IOException {
        DailyReportSummaryDTO summary = generateReportSummary(period, department);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(summary.isSingleDay() ? "Daily Meal Report" : "Meal Report");

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle boldStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            int rowIdx = 0;

            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowIdx++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(summary.getCompanyName() + (summary.isSingleDay() ? " - DAILY MEAL REPORT" : " - MEAL REPORT"));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            rowIdx++;

            org.apache.poi.ss.usermodel.Row r1 = sheet.createRow(rowIdx++);
            r1.createCell(0).setCellValue(summary.isSingleDay() ? "Report Date:" : "Report Period:");
            r1.createCell(1).setCellValue(summary.getFormattedReportDate());
            r1.getCell(0).setCellStyle(boldStyle);

            org.apache.poi.ss.usermodel.Row r2 = sheet.createRow(rowIdx++);
            r2.createCell(0).setCellValue("Total Active Staff:");
            r2.createCell(1).setCellValue(summary.getTotalEmployees());
            r2.getCell(0).setCellStyle(boldStyle);

            org.apache.poi.ss.usermodel.Row r3 = sheet.createRow(rowIdx++);
            r3.createCell(0).setCellValue("Recorded Ate:");
            r3.createCell(1).setCellValue(summary.getAteCount());
            r3.getCell(0).setCellStyle(boldStyle);

            org.apache.poi.ss.usermodel.Row r4 = sheet.createRow(rowIdx++);
            r4.createCell(0).setCellValue("Recorded Did Not Eat:");
            r4.createCell(1).setCellValue(summary.getDidNotEatCount());
            r4.getCell(0).setCellStyle(boldStyle);

            org.apache.poi.ss.usermodel.Row r5 = sheet.createRow(rowIdx++);
            r5.createCell(0).setCellValue("Total Expenditure:");
            r5.createCell(1).setCellValue(summary.getTotalExpenditure().toPlainString() + " " + summary.getCurrency());
            r5.getCell(0).setCellStyle(boldStyle);

            rowIdx++;

            org.apache.poi.ss.usermodel.Row tableHeader = sheet.createRow(rowIdx++);
            String[] headers = {"Date", "Employee Code", "Employee Name", "Position", "Department", "Meal Status", "Amount (" + summary.getCurrency() + ")", "Recorded By", "Time"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = tableHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (summary.getRecords().isEmpty()) {
                org.apache.poi.ss.usermodel.Row emptyRow = sheet.createRow(rowIdx++);
                org.apache.poi.ss.usermodel.Cell emptyCell = emptyRow.createCell(0);
                emptyCell.setCellValue(summary.isSingleDay()
                        ? "No meal records found for the selected date."
                        : "No meal records found for the selected period.");
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 8));
            } else {
                for (MealRecordDTO dto : summary.getRecords()) {
                    org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowIdx++);
                    dataRow.createCell(0).setCellValue(dto.getMealDate() != null ? dto.getMealDate().format(DATE_FORMATTER) : "-");
                    dataRow.createCell(1).setCellValue(dto.getEmployeeCode());
                    dataRow.createCell(2).setCellValue(dto.getEmployeeName());
                    dataRow.createCell(3).setCellValue(dto.getPosition() != null ? dto.getPosition() : "Worker");
                    dataRow.createCell(4).setCellValue(dto.getDepartment());
                    dataRow.createCell(5).setCellValue(dto.getMealStatus().name());
                    dataRow.createCell(6).setCellValue(dto.getAmount().doubleValue());
                    dataRow.createCell(7).setCellValue(dto.getRecordedBy());
                    dataRow.createCell(8).setCellValue(dto.getCreatedAt() != null ? dto.getCreatedAt().format(TIME_FORMATTER) : "-");
                }
            }

            org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowIdx++);
            totalRow.createCell(0).setCellValue("TOTAL EXPENDITURE");
            totalRow.getCell(0).setCellStyle(boldStyle);
            org.apache.poi.ss.usermodel.Cell totalAmountCell = totalRow.createCell(6);
            totalAmountCell.setCellValue(summary.getTotalExpenditure().doubleValue());
            totalAmountCell.setCellStyle(boldStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** PDF export with exactly the records of {@link #generateReportSummary} for the same period. */
    @Transactional(readOnly = true)
    public byte[] generatePdfReport(ReportPeriod period, String department) {
        DailyReportSummaryDTO summary = generateReportSummary(period, department);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, java.awt.Color.BLACK);
            Paragraph title = new Paragraph(summary.getCompanyName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            com.lowagie.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new java.awt.Color(0, 51, 102));
            Paragraph subTitle = new Paragraph(summary.isSingleDay() ? "DAILY EMPLOYEE MEAL REPORT" : "EMPLOYEE MEAL REPORT", subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(15);
            document.add(subTitle);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(15);
            summaryTable.setWidths(new float[]{40, 60});

            addSummaryRow(summaryTable, summary.isSingleDay() ? "Report Date:" : "Report Period:", summary.getFormattedReportDate());
            addSummaryRow(summaryTable, "Total Active Staff:", String.valueOf(summary.getTotalEmployees()));
            addSummaryRow(summaryTable, "Recorded Ate:", String.valueOf(summary.getAteCount()));
            addSummaryRow(summaryTable, "Recorded Did Not Eat:", String.valueOf(summary.getDidNotEatCount()));
            addSummaryRow(summaryTable, "Total Expenditure:", summary.getTotalExpenditure() + " " + summary.getCurrency());
            addSummaryRow(summaryTable, "Average Meal Cost:", summary.getAverageMealCost() + " " + summary.getCurrency());

            document.add(summaryTable);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{13, 9, 17, 14, 13, 12, 12, 11, 7});

            String[] headers = {"Date", "Code", "Name", "Position", "Department", "Status", "Amount", "Recorded By", "Time"};
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            java.awt.Color headerBg = new java.awt.Color(15, 23, 42);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9, java.awt.Color.BLACK);
            com.lowagie.text.Font boldDataFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.BLACK);

            if (summary.getRecords().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase(summary.isSingleDay()
                        ? "No meal records found for the selected date."
                        : "No meal records found for the selected period.", dataFont));
                emptyCell.setColspan(9);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(12);
                table.addCell(emptyCell);
            } else {
                for (MealRecordDTO r : summary.getRecords()) {
                    table.addCell(createPdfCell(r.getMealDate() != null ? r.getMealDate().format(DATE_FORMATTER) : "-", dataFont, Element.ALIGN_CENTER));
                    table.addCell(createPdfCell(r.getEmployeeCode(), dataFont, Element.ALIGN_LEFT));
                    table.addCell(createPdfCell(r.getEmployeeName(), dataFont, Element.ALIGN_LEFT));
                    table.addCell(createPdfCell(r.getPosition() != null ? r.getPosition() : "Worker", dataFont, Element.ALIGN_LEFT));
                    table.addCell(createPdfCell(r.getDepartment(), dataFont, Element.ALIGN_LEFT));
                    
                    PdfPCell statusCell = createPdfCell(r.getMealStatus().name(), boldDataFont, Element.ALIGN_CENTER);
                    if (r.getMealStatus() == MealStatus.ATE) {
                        statusCell.setBackgroundColor(new java.awt.Color(220, 252, 231));
                    } else {
                        statusCell.setBackgroundColor(new java.awt.Color(254, 226, 226));
                    }
                    table.addCell(statusCell);

                    table.addCell(createPdfCell(r.getAmount() + " " + summary.getCurrency(), dataFont, Element.ALIGN_RIGHT));
                    table.addCell(createPdfCell(r.getRecordedBy(), dataFont, Element.ALIGN_LEFT));
                    table.addCell(createPdfCell(r.getCreatedAt() != null ? r.getCreatedAt().format(TIME_FORMATTER) : "-", dataFont, Element.ALIGN_CENTER));
                }
            }

            document.add(table);

            Paragraph footer = new Paragraph("Generated by " + summary.getCompanyName() + " on " + BusinessTime.nowForDisplay().format(DATETIME_FORMATTER),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, java.awt.Color.GRAY));
            footer.setSpacingBefore(20);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        com.lowagie.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.BLACK);
        com.lowagie.text.Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.BLACK);

        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valFont));
        c2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private PdfPCell createPdfCell(String text, com.lowagie.text.Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }
}
