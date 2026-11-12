package bg.pharmacy.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Одитна следа на наличностите. Всяка промяна (доставка/продажба) оставя ред,
 * така че наличността винаги може да се обоснове със сумата на движенията.
 */
@Repository
public class StockMovementRepository {

    public enum Reason { DELIVERY, SALE, WRITE_OFF, CORRECTION }

    private final JdbcTemplate jdbc;

    public StockMovementRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void record(long medicineId, long batchId, int delta, Reason reason,
                       Long refId, long userId) {
        jdbc.update("INSERT INTO stock_movements "
                    + "(medicine_id, batch_id, delta, reason, ref_id, user_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                medicineId, batchId, delta, reason.name(), refId, userId);
    }

    /** Сверка: сумата на движенията трябва да е равна на текущата наличност. */
    public int movementSumFor(long medicineId) {
        Integer sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(delta), 0) FROM stock_movements WHERE medicine_id = ?",
                Integer.class, medicineId);
        return sum == null ? 0 : sum;
    }
}
