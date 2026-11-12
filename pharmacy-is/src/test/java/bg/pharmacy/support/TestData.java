package bg.pharmacy.support;

import bg.pharmacy.model.Role;
import bg.pharmacy.model.User;
import bg.pharmacy.service.AppUserPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/** Помощни методи за подготовка на данни в тестовете. */
public final class TestData {

    public static final long MANAGER_ID = 1L;
    public static final long PHARMACIST_ID = 2L;

    private TestData() { }

    /** Изчиства всички таблици в ред, който не нарушава външните ключове. */
    public static void clean(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM stock_movements");
        jdbc.update("DELETE FROM sale_items");
        jdbc.update("DELETE FROM sales");
        jdbc.update("DELETE FROM batches");
        jdbc.update("DELETE FROM deliveries");
        jdbc.update("DELETE FROM medicines");
        jdbc.update("DELETE FROM suppliers");
        jdbc.update("DELETE FROM users");
    }

    /** BCrypt хеш на "manager123" и "pharma123" - същите като в демо данните. */
    public static void insertUsers(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO users (id, username, password_hash, full_name, role, active) "
                    + "VALUES (?, ?, ?, ?, ?, TRUE)",
                MANAGER_ID, "manager",
                "$2a$10$fgjxhK1y775ME0gQH8kKBOkoOJyvMkzp8VlccAy.Bg6Xh.woq1Ciq",
                "Мария Георгиева", "MANAGER");
        jdbc.update("INSERT INTO users (id, username, password_hash, full_name, role, active) "
                    + "VALUES (?, ?, ?, ?, ?, TRUE)",
                PHARMACIST_ID, "pharmacist",
                "$2a$10$VCl.q75pI/5eI/8kHJWgwe26/9hxspXUoqtTRe7/BPRnRUdKbemqu",
                "Иван Петров", "PHARMACIST");
    }

    public static long insertSupplier(JdbcTemplate jdbc, String name) {
        jdbc.update("INSERT INTO suppliers (name, active) VALUES (?, TRUE)", name);
        return jdbc.queryForObject("SELECT id FROM suppliers WHERE name = ?", Long.class, name);
    }

    public static long insertMedicine(JdbcTemplate jdbc, String name, String ingredient,
                                      BigDecimal price, int minQuantity) {
        jdbc.update("INSERT INTO medicines (name, active_ingredient, form, unit_price, "
                    + "min_quantity, prescription_required, active) "
                    + "VALUES (?, ?, 'таблетки', ?, ?, FALSE, TRUE)",
                name, ingredient, price, minQuantity);
        return jdbc.queryForObject("SELECT id FROM medicines WHERE name = ?", Long.class, name);
    }

    public static long insertDelivery(JdbcTemplate jdbc, long supplierId) {
        jdbc.update("INSERT INTO deliveries (supplier_id, delivery_date, user_id) VALUES (?, ?, ?)",
                supplierId, Date.valueOf(LocalDate.now()), MANAGER_ID);
        return jdbc.queryForObject("SELECT MAX(id) FROM deliveries", Long.class);
    }

    public static long insertBatch(JdbcTemplate jdbc, long medicineId, long deliveryId,
                                   String batchNo, LocalDate expiry, int quantity,
                                   BigDecimal unitCost) {
        jdbc.update("INSERT INTO batches (medicine_id, delivery_id, batch_no, expiry_date, "
                    + "quantity_received, quantity_available, unit_cost) VALUES (?, ?, ?, ?, ?, ?, ?)",
                medicineId, deliveryId, batchNo, Date.valueOf(expiry), quantity, quantity, unitCost);
        Long batchId = jdbc.queryForObject("SELECT MAX(id) FROM batches", Long.class);
        jdbc.update("INSERT INTO stock_movements (medicine_id, batch_id, delta, reason, ref_id, user_id) "
                    + "VALUES (?, ?, ?, 'DELIVERY', ?, ?)",
                medicineId, batchId, quantity, deliveryId, MANAGER_ID);
        return batchId;
    }

    public static AppUserPrincipal manager() { return principal(MANAGER_ID, "manager", Role.MANAGER); }

    public static AppUserPrincipal pharmacist() {
        return principal(PHARMACIST_ID, "pharmacist", Role.PHARMACIST);
    }

    private static AppUserPrincipal principal(long id, String username, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash("{noop}x");
        u.setFullName(username);
        u.setRole(role);
        u.setActive(true);
        return new AppUserPrincipal(u);
    }
}
