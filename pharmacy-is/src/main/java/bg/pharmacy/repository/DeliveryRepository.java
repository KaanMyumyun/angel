package bg.pharmacy.repository;

import bg.pharmacy.model.Batch;
import bg.pharmacy.model.Delivery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class DeliveryRepository {

    /** Връщаме само първичния ключ - иначе драйвер, който връща и други
     *  генерирани колони, би счупил KeyHolder.getKey(). */
    private static final String[] ID_COLUMN = {"id"};

    private final JdbcTemplate jdbc;

    public DeliveryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(Delivery d) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO deliveries (supplier_id, delivery_date, document_no, user_id, note) "
                    + "VALUES (?, ?, ?, ?, ?)", ID_COLUMN);
            ps.setLong(1, d.getSupplierId());
            ps.setDate(2, Date.valueOf(d.getDeliveryDate()));
            ps.setString(3, d.getDocumentNo());
            ps.setLong(4, d.getUserId());
            ps.setString(5, d.getNote());
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    public List<Delivery> findRecent(int limit) {
        return jdbc.query(
                "SELECT d.id, d.supplier_id, d.delivery_date, d.document_no, d.user_id, d.note, "
                + "       s.name AS supplier_name, u.full_name AS user_name "
                + "FROM deliveries d "
                + "JOIN suppliers s ON s.id = d.supplier_id "
                + "JOIN users u ON u.id = d.user_id "
                + "ORDER BY d.delivery_date DESC, d.id DESC LIMIT ?",
                RowMappers.DELIVERY, limit);
    }

    public Optional<Delivery> findById(long id) {
        return jdbc.query(
                "SELECT d.id, d.supplier_id, d.delivery_date, d.document_no, d.user_id, d.note, "
                + "       s.name AS supplier_name, u.full_name AS user_name "
                + "FROM deliveries d "
                + "JOIN suppliers s ON s.id = d.supplier_id "
                + "JOIN users u ON u.id = d.user_id WHERE d.id = ?",
                RowMappers.DELIVERY, id).stream().findFirst();
    }

    /** Партидите, създадени от дадена доставка = доставените артикули. */
    public List<Batch> findBatches(long deliveryId) {
        return jdbc.query(
                "SELECT b.id, b.medicine_id, b.delivery_id, b.batch_no, b.expiry_date, "
                + "b.quantity_received, b.quantity_available, b.unit_cost, m.name AS medicine_name "
                + "FROM batches b JOIN medicines m ON m.id = b.medicine_id "
                + "WHERE b.delivery_id = ? ORDER BY m.name",
                RowMappers.BATCH, deliveryId);
    }
}
