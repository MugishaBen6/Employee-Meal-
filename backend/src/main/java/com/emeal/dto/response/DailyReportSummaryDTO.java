package com.emeal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DailyReportSummaryDTO {

    private LocalDate reportDate;
    private String formattedReportDate;
    private String companyName;
    private long totalEmployees;
    private long ateCount;
    private long didNotEatCount;
    private BigDecimal totalExpenditure;
    private BigDecimal averageMealCost;
    private String currency;
    private List<MealRecordDTO> records;

    public DailyReportSummaryDTO() {
    }

    public DailyReportSummaryDTO(LocalDate reportDate, String formattedReportDate, String companyName, long totalEmployees, long ateCount, long didNotEatCount, BigDecimal totalExpenditure, BigDecimal averageMealCost, String currency, List<MealRecordDTO> records) {
        this.reportDate = reportDate;
        this.formattedReportDate = formattedReportDate;
        this.companyName = companyName;
        this.totalEmployees = totalEmployees;
        this.ateCount = ateCount;
        this.didNotEatCount = didNotEatCount;
        this.totalExpenditure = totalExpenditure;
        this.averageMealCost = averageMealCost;
        this.currency = currency;
        this.records = records;
    }

    public static DailyReportSummaryDTOBuilder builder() {
        return new DailyReportSummaryDTOBuilder();
    }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public String getFormattedReportDate() { return formattedReportDate; }
    public void setFormattedReportDate(String formattedReportDate) { this.formattedReportDate = formattedReportDate; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getAteCount() { return ateCount; }
    public void setAteCount(long ateCount) { this.ateCount = ateCount; }

    public long getDidNotEatCount() { return didNotEatCount; }
    public void setDidNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; }

    public BigDecimal getTotalExpenditure() { return totalExpenditure; }
    public void setTotalExpenditure(BigDecimal totalExpenditure) { this.totalExpenditure = totalExpenditure; }

    public BigDecimal getAverageMealCost() { return averageMealCost; }
    public void setAverageMealCost(BigDecimal averageMealCost) { this.averageMealCost = averageMealCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<MealRecordDTO> getRecords() { return records; }
    public void setRecords(List<MealRecordDTO> records) { this.records = records; }

    public static class DailyReportSummaryDTOBuilder {
        private LocalDate reportDate;
        private String formattedReportDate;
        private String companyName;
        private long totalEmployees;
        private long ateCount;
        private long didNotEatCount;
        private BigDecimal totalExpenditure;
        private BigDecimal averageMealCost;
        private String currency;
        private List<MealRecordDTO> records;

        DailyReportSummaryDTOBuilder() {}

        public DailyReportSummaryDTOBuilder reportDate(LocalDate reportDate) { this.reportDate = reportDate; return this; }
        public DailyReportSummaryDTOBuilder formattedReportDate(String formattedReportDate) { this.formattedReportDate = formattedReportDate; return this; }
        public DailyReportSummaryDTOBuilder companyName(String companyName) { this.companyName = companyName; return this; }
        public DailyReportSummaryDTOBuilder totalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; return this; }
        public DailyReportSummaryDTOBuilder ateCount(long ateCount) { this.ateCount = ateCount; return this; }
        public DailyReportSummaryDTOBuilder didNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; return this; }
        public DailyReportSummaryDTOBuilder totalExpenditure(BigDecimal totalExpenditure) { this.totalExpenditure = totalExpenditure; return this; }
        public DailyReportSummaryDTOBuilder averageMealCost(BigDecimal averageMealCost) { this.averageMealCost = averageMealCost; return this; }
        public DailyReportSummaryDTOBuilder currency(String currency) { this.currency = currency; return this; }
        public DailyReportSummaryDTOBuilder records(List<MealRecordDTO> records) { this.records = records; return this; }

        public DailyReportSummaryDTO build() {
            return new DailyReportSummaryDTO(reportDate, formattedReportDate, companyName, totalEmployees, ateCount, didNotEatCount, totalExpenditure, averageMealCost, currency, records);
        }
    }
}
