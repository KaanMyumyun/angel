package bg.pharmacy.repository;

import bg.pharmacy.model.Batch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class BatchRepository {

    /** Връщаме само първичния ключ - иначе драйвер, който връща и други
     *  генерирани колони, би счупил KeyHolder.getKey(). */
    private static final String[] ID_COLUMN = {"id"};

    private static final String COLUMNS =
            "id, medicine_id, delivery_id, batch_no, expiry_date, "
            + "quantity_received, quantity_available, unit_cost";

    private final JdbcTemplate jdbc;

    public BatchRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Партидите на едно лекарство, ЗАКЛЮЧЕНИ до края на транзакцията (FOR UPDATE).
     * Така две едновременни продажби на последната кутия не могат да минат заедно.
     */
    public List<Batch> findForUpdate(long medicineId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM batches "
                          + "WHERE medicine_id = ? AND quantity_available > 0 "
                          + "ORDER BY expiry_date, id FOR UPDATE",
                RowMappers.BATCH, medicineId);
    }

    public List<Batch> findByMedicine(long medicineId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM batches WHERE medicine_id = ? "
                          + "ORDER BY expiry_date, id", RowMappers.BATCH, medicineId);
    }

    public List<Batch> findAllWithStock() {
        return jdbc.query(
                "SELECT b.id, b.medicine_id, b.delivery_id, b.batch_no, b.expiry_date, "
                + "b.quantity_received, b.quantity_available, b.unit_cost, m.name AS medicine_name "
                + "FROM batches b JOIN medicines m ON m.id = b.medicine_id "
                + "WHERE b.quantity_available > 0 ORDER BY b.expiry_date, m.name",
                RowMappers.BATCH);
    }

    public long insert(Batch b) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO batches (medicine_id, delivery_id, batch_no, expiry_date, "
                    + "quantity_received, quantity_available, unit_cost) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    ID_COLUMN);
            ps.setLong(1, b.getMedicineId());
            ps.setLong(2, b.getDeliveryId());
            ps.setString(3, b.getBatchNo());
            ps.setDate(4, Date.valueOf(b.getExpiryDate()));
            ps.setInt(5, b.getQuantityReceived());
            ps.setInt(6, b.getQuantityAvailable());
            ps.setBigDecimal(7, b.getUnitCost());
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    /**
     * Намалява наличността на партида. Условието quantity_available >= ?
     * е втора защита: при разминаване UPDATE-ът засяга 0 реда и
     * извикващият код прекъсва транзакцията, вместо да запише отрицателна наличност.
     *
     * @return брой засегнати редове (1 = успех, 0 = недостатъчна наличност)
     */
    public int decrease(long batchId, int quantity) {
        return jdbc.update(
                "UPDATE batches SET quantity_available = quantity_available - ? "
                + "WHERE id = ? AND quantity_available >= ?",
                quantity, batchId, quantity);
    }
}
