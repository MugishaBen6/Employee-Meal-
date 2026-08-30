package com.emeal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseChartData {

    private LocalDate date;
    private String formattedDate;
    private BigDecimal amount;
    private long ateCount;
    private long didNotEatCount;

    public ExpenseChartData() {
    }

    public ExpenseChartData(LocalDate date, String formattedDate, BigDecimal amount, long ateCount, long didNotEatCount) {
        this.date = date;
        this.formattedDate = formattedDate;
        this.amount = amount;
        this.ateCount = ateCount;
        this.didNotEatCount = didNotEatCount;
    }

    public static ExpenseChartDataBuilder builder() {
        return new ExpenseChartDataBuilder();
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getFormattedDate() { return formattedDate; }
    public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public long getAteCount() { return ateCount; }
    public void setAteCount(long ateCount) { this.ateCount = ateCount; }

    public long getDidNotEatCount() { return didNotEatCount; }
    public void setDidNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; }

    public static class ExpenseChartDataBuilder {
        private LocalDate date;
        private String formattedDate;
        private BigDecimal amount;
        private long ateCount;
        private long didNotEatCount;

        ExpenseChartDataBuilder() {}

        public ExpenseChartDataBuilder date(LocalDate date) { this.date = date; return this; }
        public ExpenseChartDataBuilder formattedDate(String formattedDate) { this.formattedDate = formattedDate; return this; }
        public ExpenseChartDataBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public ExpenseChartDataBuilder ateCount(long ateCount) { this.ateCount = ateCount; return this; }
        public ExpenseChartDataBuilder didNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; return this; }

        public ExpenseChartData build() {
            return new ExpenseChartData(date, formattedDate, amount, ateCount, didNotEatCount);
        }
    }
}
