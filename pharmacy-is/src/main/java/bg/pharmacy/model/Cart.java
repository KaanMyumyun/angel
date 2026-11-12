package bg.pharmacy.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Кошницата се пази в HTTP сесията на фармацевта, затова е Serializable.
 * Съдържа само намерения за продажба - наличността се заделя чак при приключване.
 */
public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<CartLine> lines = new ArrayList<>();

    public List<CartLine> getLines() { return lines; }

    /** Добавянето на вече наличен артикул увеличава количеството, вместо да дублира реда. */
    public void add(long medicineId, String medicineName, BigDecimal unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количеството трябва да е положително число.");
        }
        for (CartLine line : lines) {
            if (line.getMedicineId() == medicineId) {
                line.setQuantity(line.getQuantity() + quantity);
                return;
            }
        }
        lines.add(new CartLine(medicineId, medicineName, unitPrice, quantity));
    }

    public void updateQuantity(long medicineId, int quantity) {
        if (quantity <= 0) {
            remove(medicineId);
            return;
        }
        lines.stream()
             .filter(l -> l.getMedicineId() == medicineId)
             .findFirst()
             .ifPresent(l -> l.setQuantity(quantity));
    }

    public void remove(long medicineId) {
        lines.removeIf(l -> l.getMedicineId() == medicineId);
    }

    public void clear() { lines.clear(); }

    public boolean isEmpty() { return lines.isEmpty(); }

    public int getItemCount() {
        return lines.stream().mapToInt(CartLine::getQuantity).sum();
    }

    public BigDecimal getTotal() {
        return lines.stream()
                    .map(CartLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
    }
}
