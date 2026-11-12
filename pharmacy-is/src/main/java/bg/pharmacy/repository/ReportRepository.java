package bg.pharmacy.repository;

import bg.pharmacy.model.ProfitSummary;
import bg.pharmacy.model.TopSellerRow;
import bg.pharmacy.model.TurnoverRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/** Справки за управителя. Периодът винаги се подава като параметри. */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Дневен оборот по дни за period [from, to]. */
    public List<TurnoverRow> dailyTurnover(LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT CAST(s.sold_at AS DATE) AS sale_day, COUNT(DISTINCT s.id) AS sales_count, "
                + "       COALESCE(SUM(si.quantity), 0) AS item_count, "
                + "       COALESCE(SUM(si.line_total), 0) AS total "
                + "FROM sales s LEFT JOIN sale_items si ON si.sale_id = s.id "
                + "WHERE s.sold_at >= ? AND s.sold_at < ? "
                + "GROUP BY CAST(s.sold_at AS DATE) ORDER BY sale_day DESC",
                (rs, i) -> {
                    TurnoverRow r = new TurnoverRow();
                    r.setDay(rs.getDate("sale_day").toLocalDate());
                    r.setSalesCount(rs.getInt("sales_count"));
                    r.setItemCount(rs.getInt("item_count"));
                    r.setTotal(rs.getBigDecimal("total"));
                    return r;
                }, startOf(from), startOfNextDay(to));
    }

    public List<TopSellerRow> topSellers(LocalDate from, LocalDate to, int limit) {
        return jdbc.query(
                "SELECT m.id AS medicine_id, m.name AS medicine_name, m.active_ingredient, "
                + "       SUM(si.quantity) AS quantity_sold, SUM(si.line_total) AS revenue "
                + "FROM sale_items si "
                + "JOIN sales s ON s.id = si.sale_id "
                + "JOIN medicines m ON m.id = si.medicine_id "
                + "WHERE s.sold_at >= ? AND s.sold_at < ? "
                + "GROUP BY m.id, m.name, m.active_ingredient "
                + "ORDER BY quantity_sold DESC, revenue DESC LIMIT ?",
                (rs, i) -> {
                    TopSellerRow r = new TopSellerRow();
                    r.setMedicineId(rs.getLong("medicine_id"));
                    r.setMedicineName(rs.getString("medicine_name"));
                    r.setActiveIngredient(rs.getString("active_ingredient"));
                    r.setQuantitySold(rs.getInt("quantity_sold"));
                    r.setRevenue(rs.getBigDecimal("revenue"));
                    return r;
                }, startOf(from), startOfNextDay(to), limit);
    }

    /**
     * Приход и себестойност за период. Себестойността се взема от партидата,
     * от която реално е продадено (batch.unit_cost), а не осреднено.
     */
    public ProfitSummary profit(LocalDate from, LocalDate to) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(si.line_total), 0) AS revenue, "
                + "       COALESCE(SUM(si.quantity * b.unit_cost), 0) AS cost, "
                + "       COUNT(DISTINCT s.id) AS sales_count, "
                + "       COALESCE(SUM(si.quantity), 0) AS item_count "
                + "FROM sales s "
                + "JOIN sale_items si ON si.sale_id = s.id "
                + "JOIN batches b ON b.id = si.batch_id "
                + "WHERE s.sold_at >= ? AND s.sold_at < ?",
                (rs, i) -> {
                    ProfitSummary p = new ProfitSummary();
                    p.setRevenue(rs.getBigDecimal("revenue"));
                    p.setCost(rs.getBigDecimal("cost"));
                    p.setSalesCount(rs.getInt("sales_count"));
                    p.setItemCount(rs.getInt("item_count"));
                    return p;
                }, startOf(from), startOfNextDay(to));
    }

    /**
     * Периодът се подава като два момента във времето вместо DATE(sold_at),
     * за да е стандартен SQL и да може да ползва индекса по sold_at.
     */
    private static Timestamp startOf(LocalDate day) {
        return Timestamp.valueOf(day.atStartOfDay());
    }

    private static Timestamp startOfNextDay(LocalDate day) {
        return Timestamp.valueOf(day.plusDays(1).atStartOfDay());
    }

    /** Стойност на склада по доставни цени. */
    public BigDecimal stockValue() {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantity_available * unit_cost), 0) FROM batches "
                + "WHERE expiry_date >= CURRENT_DATE", BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
