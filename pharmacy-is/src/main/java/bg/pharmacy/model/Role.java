package bg.pharmacy.model;

/** Нива на достъп. Управителят вижда справките, фармацевтът - само ежедневните операции. */
public enum Role {
    MANAGER("Управител"),
    PHARMACIST("Фармацевт");

    private final String label;
    Role(String label) { this.label = label; }
    public String getLabel() { return label; }
}
