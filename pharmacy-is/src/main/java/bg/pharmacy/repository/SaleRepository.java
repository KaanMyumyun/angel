package bg.pharmacy.repository;

import bg.pharmacy.model.Sale;
import bg.pharmacy.model.SaleItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class SaleRepository {

    /** Връщаме само първичния ключ - иначе драйвер, който връща и други
     *  генерирани колони, би счупил KeyHolder.getKey(). */
    private static final String[] ID_COLUMN = {"id"};

    private final JdbcTemplate jdbc;

    public SaleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insertSale(long userId, BigDecimal total) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sales (user_id, total) VALUES (?, ?)",
                    ID_COLUMN);
            ps.setLong(1, userId);
            ps.setBigDecimal(2, total);
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    public void insertItem(long saleId, long medicineId, long batchId,
                           int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        jdbc.update("INSERT INTO sale_items "
                    + "(sale_id, medicine_id, batch_id, quantity, unit_price, line_total) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                saleId, medicineId, batchId, quantity, unitPrice, lineTotal);
    }

    public List<Sale> findRecent(int limit) {
        return jdbc.query(
                "SELECT s.id, s.sold_at, s.user_id, s.total, u.full_name AS user_name "
                + "FROM sales s JOIN users u ON u.id = s.user_id "
                + "ORDER BY s.sold_at DESC, s.id DESC LIMIT ?",
                RowMappers.SALE, limit);
    }

    public Optional<Sale> findById(long id) {
        Optional<Sale> sale = jdbc.query(
                "SELECT s.id, s.sold_at, s.user_id, s.total, u.full_name AS user_name "
                + "FROM sales s JOIN users u ON u.id = s.user_id WHERE s.id = ?",
                RowMappers.SALE, id).stream().findFirst();
        sale.ifPresent(s -> s.setItems(findItems(id)));
        return sale;
    }

    public List<SaleItem> findItems(long saleId) {
        return jdbc.query(
                "SELECT si.id, si.sale_id, si.medicine_id, si.batch_id, si.quantity, "
                + "       si.unit_price, si.line_total, m.name AS medicine_name, b.batch_no "
                + "FROM sale_items si "
                + "JOIN medicines m ON m.id = si.medicine_id "
                + "JOIN batches b ON b.id = si.batch_id "
                + "WHERE si.sale_id = ? ORDER BY si.id",
                RowMappers.SALE_ITEM, saleId);
    }
}
