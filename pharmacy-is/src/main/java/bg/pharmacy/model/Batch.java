package bg.pharmacy.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Партида = един ред от една доставка, със собствен срок на годност.
 * quantityReceived не се променя (за справки), quantityAvailable намалява при продажба.
 */
public class Batch {
    private Long id;
    private long medicineId;
    private String medicineName;
    private long deliveryId;
    private String batchNo;
    private LocalDate expiryDate;
    private int quantityReceived;
    private int quantityAvailable;
    private BigDecimal unitCost;

    public Batch() { }

    public Batch(Long id, long medicineId, LocalDate expiryDate, int quantityAvailable) {
        this.id = id;
        this.medicineId = medicineId;
        this.expiryDate = expiryDate;
        this.quantityAvailable = quantityAvailable;
        this.quantityReceived = quantityAvailable;
    }

    public boolean isExpired(LocalDate today) { return expiryDate.isBefore(today); }

    public long daysUntilExpiry(LocalDate today) {
        return java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(long deliveryId) { this.deliveryId = deliveryId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }
    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
