package com.emeal.dto.response;

import java.math.BigDecimal;

/**
 * Dashboard statistics scoped to a single USER — contains only records
 * created by (recorded_by) the authenticated user.
 */
public class UserDashboardStatsResponse {

    private String username;
    private long totalRecords;
    private long ateCount;
    private long didNotEatCount;
    private BigDecimal totalAmount;
    private String currency;
    private BigDecimal standardMealPrice;

    public UserDashboardStatsResponse() {}

    public UserDashboardStatsResponse(String username, long totalRecords, long ateCount,
                                      long didNotEatCount, BigDecimal totalAmount, String currency,
                                      BigDecimal standardMealPrice) {
        this.username = username;
        this.totalRecords = totalRecords;
        this.ateCount = ateCount;
        this.didNotEatCount = didNotEatCount;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.standardMealPrice = standardMealPrice;
    }

    public static UserDashboardStatsResponseBuilder builder() {
        return new UserDashboardStatsResponseBuilder();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getAteCount() { return ateCount; }
    public void setAteCount(long ateCount) { this.ateCount = ateCount; }

    public long getDidNotEatCount() { return didNotEatCount; }
    public void setDidNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getStandardMealPrice() { return standardMealPrice; }
    public void setStandardMealPrice(BigDecimal standardMealPrice) { this.standardMealPrice = standardMealPrice; }

    public static class UserDashboardStatsResponseBuilder {
        private String username;
        private long totalRecords;
        private long ateCount;
        private long didNotEatCount;
        private BigDecimal totalAmount;
        private String currency;
        private BigDecimal standardMealPrice;

        UserDashboardStatsResponseBuilder() {}

        public UserDashboardStatsResponseBuilder username(String username) { this.username = username; return this; }
        public UserDashboardStatsResponseBuilder totalRecords(long totalRecords) { this.totalRecords = totalRecords; return this; }
        public UserDashboardStatsResponseBuilder ateCount(long ateCount) { this.ateCount = ateCount; return this; }
        public UserDashboardStatsResponseBuilder didNotEatCount(long didNotEatCount) { this.didNotEatCount = didNotEatCount; return this; }
        public UserDashboardStatsResponseBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public UserDashboardStatsResponseBuilder currency(String currency) { this.currency = currency; return this; }
        public UserDashboardStatsResponseBuilder standardMealPrice(BigDecimal standardMealPrice) { this.standardMealPrice = standardMealPrice; return this; }

        public UserDashboardStatsResponse build() {
            return new UserDashboardStatsResponse(username, totalRecords, ateCount, didNotEatCount, totalAmount, currency, standardMealPrice);
        }
    }
}
