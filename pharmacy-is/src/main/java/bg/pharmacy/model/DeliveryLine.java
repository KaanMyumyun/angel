package bg.pharmacy.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Един доставен артикул - при записване става партида. */
public class DeliveryLine {
    @NotNull(message = "Изберете лекарство")
    private Long medicineId;

    private String medicineName;

    @Size(max = 50) private String batchNo;

    @NotNull(message = "Срокът на годност е задължителен")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @NotNull(message = "Количеството е задължително")
    @Min(value = 1, message = "Количеството трябва да е поне 1")
    private Integer quantity;

    @NotNull(message = "Доставната цена е задължителна")
    @DecimalMin(value = "0.00", message = "Доставната цена не може да е отрицателна")
    private BigDecimal unitCost;

    /** Стойност на реда (количество x доставна цена) - смята се тук, не в шаблона. */
    public BigDecimal getLineValue() {
        if (unitCost == null || quantity == null) return BigDecimal.ZERO;
        return unitCost.multiply(BigDecimal.valueOf(quantity.longValue()));
    }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
