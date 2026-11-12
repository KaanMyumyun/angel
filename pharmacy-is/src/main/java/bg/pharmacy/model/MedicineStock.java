package bg.pharmacy.model;

import java.time.LocalDate;

/** Лекарство заедно с обобщената му наличност (сбор от партидите). */
public class MedicineStock {
    private Medicine medicine;
    private int sellableQuantity;   // само от неизтекли партиди
    private int expiredQuantity;
    private LocalDate nearestExpiry;

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }
    public int getSellableQuantity() { return sellableQuantity; }
    public void setSellableQuantity(int sellableQuantity) { this.sellableQuantity = sellableQuantity; }
    public int getExpiredQuantity() { return expiredQuantity; }
    public void setExpiredQuantity(int expiredQuantity) { this.expiredQuantity = expiredQuantity; }
    public LocalDate getNearestExpiry() { return nearestExpiry; }
    public void setNearestExpiry(LocalDate nearestExpiry) { this.nearestExpiry = nearestExpiry; }

    public int getTotalQuantity() { return sellableQuantity + expiredQuantity; }

    public boolean isLowStock() {
        return medicine != null && sellableQuantity < medicine.getMinQuantity();
    }
}
