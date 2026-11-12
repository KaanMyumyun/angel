package bg.pharmacy.repository;

import bg.pharmacy.model.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class SupplierRepository {

    /** Връщаме само първичния ключ - иначе драйвер, който връща и други
     *  генерирани колони, би счупил KeyHolder.getKey(). */
    private static final String[] ID_COLUMN = {"id"};

    private static final String COLUMNS =
            "id, name, eik, contact_person, phone, email, address, active";

    private final JdbcTemplate jdbc;

    public SupplierRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Supplier> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM suppliers ORDER BY name", RowMappers.SUPPLIER);
    }

    public List<Supplier> findAllActive() {
        return jdbc.query("SELECT " + COLUMNS + " FROM suppliers WHERE active = TRUE ORDER BY name",
                RowMappers.SUPPLIER);
    }

    public Optional<Supplier> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM suppliers WHERE id = ?",
                RowMappers.SUPPLIER, id).stream().findFirst();
    }

    public long insert(Supplier s) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO suppliers (name, eik, contact_person, phone, email, address, active) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)", ID_COLUMN);
            ps.setString(1, s.getName());
            ps.setString(2, s.getEik());
            ps.setString(3, s.getContactPerson());
            ps.setString(4, s.getPhone());
            ps.setString(5, s.getEmail());
            ps.setString(6, s.getAddress());
            ps.setBoolean(7, s.isActive());
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    public void update(Supplier s) {
        jdbc.update("UPDATE suppliers SET name = ?, eik = ?, contact_person = ?, phone = ?, "
                    + "email = ?, address = ?, active = ? WHERE id = ?",
                s.getName(), s.getEik(), s.getContactPerson(), s.getPhone(),
                s.getEmail(), s.getAddress(), s.isActive(), s.getId());
    }

    /** Две отделни заявки вместо NULL-параметър - MySQL не типизира нетипизиран NULL надеждно. */
    public boolean nameExists(String name, Long excludeId) {
        Integer count = (excludeId == null)
                ? jdbc.queryForObject("SELECT COUNT(*) FROM suppliers WHERE name = ?",
                        Integer.class, name)
                : jdbc.queryForObject("SELECT COUNT(*) FROM suppliers WHERE name = ? AND id <> ?",
                        Integer.class, name, excludeId);
        return count != null && count > 0;
    }
}
