package bg.pharmacy.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Supplier {
    private Long id;

    @NotBlank(message = "Името на доставчика е задължително")
    @Size(max = 150, message = "Името е твърде дълго (макс. 150 символа)")
    private String name;

    @Size(max = 20, message = "ЕИК е твърде дълъг")
    private String eik;

    @Size(max = 120) private String contactPerson;
    @Size(max = 40)  private String phone;

    @Email(message = "Невалиден имейл адрес")
    @Size(max = 120) private String email;

    @Size(max = 255) private String address;

    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEik() { return eik; }
    public void setEik(String eik) { this.eik = eik; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
