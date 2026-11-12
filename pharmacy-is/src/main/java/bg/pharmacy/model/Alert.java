package bg.pharmacy.model;

import java.time.LocalDate;

/** Предупреждение, показвано на началния екран и в секция "Склад". */
public class Alert {
    public enum Type {
        LOW_STOCK("Ниска наличност"),
        EXPIRING_SOON("Изтичащ срок"),
        EXPIRED("Изтекъл срок");

        private final String label;
        Type(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private Type type;
    private long medicineId;
    private String medicineName;
    private String batchNo;
    private int quantity;
    private int minQuantity;
    private LocalDate expiryDate;
    private long daysLeft;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public long getDaysLeft() { return daysLeft; }
    public void setDaysLeft(long daysLeft) { this.daysLeft = daysLeft; }
}
