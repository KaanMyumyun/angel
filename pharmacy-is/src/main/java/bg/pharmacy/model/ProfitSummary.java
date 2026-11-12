package bg.pharmacy.model;

import java.math.BigDecimal;

/**
 * Обобщение приход / себестойност / печалба за период.
 * Себестойността идва от unit_cost на партидата, от която реално е продадено.
 */
public class ProfitSummary {
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal cost = BigDecimal.ZERO;
    private int salesCount;
    private int itemCount;

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public BigDecimal getProfit() { return revenue.subtract(cost); }

    /** Надценка в проценти спрямо себестойността. */
    public BigDecimal getMarginPercent() {
        if (revenue.signum() == 0) return BigDecimal.ZERO;
        return getProfit().multiply(BigDecimal.valueOf(100))
                          .divide(revenue, 1, java.math.RoundingMode.HALF_UP);
    }
}
