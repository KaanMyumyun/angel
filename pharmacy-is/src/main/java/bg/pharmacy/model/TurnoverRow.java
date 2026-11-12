package bg.pharmacy.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ред от справката "дневен оборот". */
public class TurnoverRow {
    private LocalDate day;
    private int salesCount;
    private int itemCount;
    private BigDecimal total;

    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }
    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
