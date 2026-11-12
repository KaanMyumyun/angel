package bg.pharmacy.model;

import java.math.BigDecimal;

/** Един ред от текущата продажба (кошницата на касата). */
public class CartLine {
    private long medicineId;
    private String medicineName;
    private BigDecimal unitPrice;
    private int quantity;

    public CartLine() { }

    public CartLine(long medicineId, String medicineName, BigDecimal unitPrice, int quantity) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
