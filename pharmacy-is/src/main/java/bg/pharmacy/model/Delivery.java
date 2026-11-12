package bg.pharmacy.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Delivery {
    private Long id;

    @NotNull(message = "Изберете доставчик")
    private Long supplierId;

    private String supplierName;

    @NotNull(message = "Датата на доставка е задължителна")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deliveryDate = LocalDate.now();

    @Size(max = 50) private String documentNo;
    @Size(max = 255) private String note;

    private long userId;
    private String userName;

    @Valid
    @NotEmpty(message = "Доставката трябва да съдържа поне един артикул")
    private List<DeliveryLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getDocumentNo() { return documentNo; }
    public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public List<DeliveryLine> getLines() { return lines; }
    public void setLines(List<DeliveryLine> lines) { this.lines = lines; }
}
