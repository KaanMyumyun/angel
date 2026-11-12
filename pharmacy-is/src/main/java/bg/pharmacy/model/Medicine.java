package bg.pharmacy.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class Medicine {
    private Long id;

    @NotBlank(message = "Наименованието е задължително")
    @Size(max = 150, message = "Наименованието е твърде дълго (макс. 150 символа)")
    private String name;

    @NotBlank(message = "Активната съставка е задължителна")
    @Size(max = 150)
    private String activeIngredient;

    @NotBlank(message = "Формата е задължителна")
    @Size(max = 40)
    private String form;

    @Size(max = 40)
    private String strength;

    @NotNull(message = "Цената е задължителна")
    @DecimalMin(value = "0.00", message = "Цената не може да е отрицателна")
    private BigDecimal unitPrice;

    @NotNull(message = "Минималната наличност е задължителна")
    @Min(value = 0, message = "Минималната наличност не може да е отрицателна")
    private Integer minQuantity = 10;

    private boolean prescriptionRequired;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String activeIngredient) { this.activeIngredient = activeIngredient; }
    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public boolean isPrescriptionRequired() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean prescriptionRequired) { this.prescriptionRequired = prescriptionRequired; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** "Аналгин 500 mg (Метамизол натрий, таблетки)" - за падащи менюта и търсене. */
    public String getDisplayName() {
        return name + " (" + activeIngredient + ", " + form + ")";
    }
}
