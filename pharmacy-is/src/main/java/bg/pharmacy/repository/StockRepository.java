package bg.pharmacy.repository;

import bg.pharmacy.model.Alert;
import bg.pharmacy.model.MedicineStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Обобщени наличности и предупреждения - изчисляват се от партидите. */
@Repository
public class StockRepository {

    private static final String STOCK_SELECT =
            "SELECT m.id, m.name, m.active_ingredient, m.form, m.strength, m.unit_price, "
            + "       m.min_quantity, m.prescription_required, m.active, "
            + "       COALESCE(SUM(CASE WHEN b.expiry_date >= CURRENT_DATE "
            + "                         THEN b.quantity_available ELSE 0 END), 0) AS sellable_quantity, "
            + "       COALESCE(SUM(CASE WHEN b.expiry_date <  CURRENT_DATE "
            + "                         THEN b.quantity_available ELSE 0 END), 0) AS expired_quantity, "
            + "       MIN(CASE WHEN b.quantity_available > 0 AND b.expiry_date >= CURRENT_DATE "
            + "                THEN b.expiry_date END) AS nearest_expiry "
            + "FROM medicines m "
            + "LEFT JOIN batches b ON b.medicine_id = m.id ";

    private static final String GROUP_BY =
            "GROUP BY m.id, m.name, m.active_ingredient, m.form, m.strength, m.unit_price, "
            + "m.min_quantity, m.prescription_required, m.active ";

    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Наличности с незадължителни филтри: текст (име / активна съставка) и доставчик.
     * Филтрите се добавят като ПАРАМЕТРИ - потребителският текст не влиза в SQL низа.
     */
    public List<MedicineStock> findStock(String term, Long supplierId) {
        StringBuilder sql = new StringBuilder(STOCK_SELECT).append("WHERE m.active = TRUE ");
        List<Object> params = new ArrayList<>();

        if (term != null && !term.isBlank()) {
            sql.append("AND (m.name LIKE ? ESCAPE '!' OR m.active_ingredient LIKE ? ESCAPE '!') ");
            String like = "%" + MedicineRepository.safeLike(term.trim()) + "%";
            params.add(like);
            params.add(like);
        }
        if (supplierId != null) {
            sql.append("AND EXISTS (SELECT 1 FROM batches b2 "
                       + "JOIN deliveries d ON d.id = b2.delivery_id "
                       + "WHERE b2.medicine_id = m.id AND d.supplier_id = ?) ");
            params.add(supplierId);
        }
        sql.append(GROUP_BY).append("ORDER BY m.name");

        return jdbc.query(sql.toString(), RowMappers.MEDICINE_STOCK, params.toArray());
    }

    public Optional<MedicineStock> findStockFor(long medicineId) {
        String sql = STOCK_SELECT + "WHERE m.id = ? " + GROUP_BY;
        return jdbc.query(sql, RowMappers.MEDICINE_STOCK, medicineId).stream().findFirst();
    }

    /** Лекарства, чиято годна наличност е под зададения минимум. */
    public List<Alert> findLowStock() {
        // Обвиваме обобщението в подзаявка: псевдоним на агрегат в HAVING
        // е разширение на MySQL и не работи в стандартен SQL.
        String sql = "SELECT * FROM (" + STOCK_SELECT + "WHERE m.active = TRUE " + GROUP_BY + ") st "
                     + "WHERE st.sellable_quantity < st.min_quantity "
                     + "ORDER BY (st.min_quantity - st.sellable_quantity) DESC, st.name";
        return jdbc.query(sql, (rs, i) -> {
            Alert a = new Alert();
            a.setType(Alert.Type.LOW_STOCK);
            a.setMedicineId(rs.getLong("id"));
            a.setMedicineName(rs.getString("name"));
            a.setQuantity(rs.getInt("sellable_quantity"));
            a.setMinQuantity(rs.getInt("min_quantity"));
            return a;
        });
    }

    private static final String ALERT_SELECT =
            "SELECT m.id AS medicine_id, m.name AS medicine_name, b.batch_no, "
            + "       b.expiry_date, b.quantity_available "
            + "FROM batches b JOIN medicines m ON m.id = b.medicine_id "
            + "WHERE b.quantity_available > 0 ";

    /**
     * Партиди с наличност, чийто срок изтича до {@code days} дни напред.
     * Границата се изчислява в Java и се подава като параметър - така заявката
     * остава стандартен SQL (без DATE_ADD/DATEDIFF) и ползва индекса по expiry_date.
     */
    public List<Alert> findExpiringSoon(int days) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(days);
        return jdbc.query(
                ALERT_SELECT + "AND b.expiry_date >= ? AND b.expiry_date <= ? "
                + "ORDER BY b.expiry_date, m.name",
                (rs, i) -> alert(rs, Alert.Type.EXPIRING_SOON, today),
                Date.valueOf(today), Date.valueOf(cutoff));
    }

    /** Партиди с наличност, които вече са изтекли - подлежат на изваждане от продажба. */
    public List<Alert> findExpired() {
        LocalDate today = LocalDate.now();
        return jdbc.query(
                ALERT_SELECT + "AND b.expiry_date < ? ORDER BY b.expiry_date, m.name",
                (rs, i) -> alert(rs, Alert.Type.EXPIRED, today),
                Date.valueOf(today));
    }

    private static Alert alert(java.sql.ResultSet rs, Alert.Type type, LocalDate today)
            throws java.sql.SQLException {
        Alert a = new Alert();
        a.setType(type);
        a.setMedicineId(rs.getLong("medicine_id"));
        a.setMedicineName(rs.getString("medicine_name"));
        a.setBatchNo(rs.getString("batch_no"));
        LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
        a.setExpiryDate(expiry);
        a.setQuantity(rs.getInt("quantity_available"));
        a.setDaysLeft(ChronoUnit.DAYS.between(today, expiry));
        return a;
    }
}
