package bg.pharmacy.repository;

import bg.pharmacy.model.Medicine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicineRepository {

    /** Връщаме само първичния ключ - иначе драйвер, който връща и други
     *  генерирани колони, би счупил KeyHolder.getKey(). */
    private static final String[] ID_COLUMN = {"id"};

    private static final String COLUMNS =
            "id, name, active_ingredient, form, strength, unit_price, "
            + "min_quantity, prescription_required, active";

    private final JdbcTemplate jdbc;

    public MedicineRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Medicine> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM medicines ORDER BY name", RowMappers.MEDICINE);
    }

    public List<Medicine> findAllActive() {
        return jdbc.query("SELECT " + COLUMNS + " FROM medicines WHERE active = TRUE ORDER BY name",
                RowMappers.MEDICINE);
    }

    public Optional<Medicine> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM medicines WHERE id = ?",
                RowMappers.MEDICINE, id).stream().findFirst();
    }

    /**
     * Търсене по име ИЛИ по активна съставка.
     * Шаблонът за LIKE се подава като параметър - текстът от потребителя
     * никога не се залепва в SQL низа.
     */
    public List<Medicine> search(String term) {
        String like = "%" + safeLike(term) + "%";
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM medicines "
                + "WHERE active = TRUE AND (name LIKE ? ESCAPE '!' OR active_ingredient LIKE ? ESCAPE '!') "
                + "ORDER BY name LIMIT 50",
                RowMappers.MEDICINE, like, like);
    }

    public long insert(Medicine m) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO medicines (name, active_ingredient, form, strength, unit_price, "
                    + "min_quantity, prescription_required, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    ID_COLUMN);
            ps.setString(1, m.getName());
            ps.setString(2, m.getActiveIngredient());
            ps.setString(3, m.getForm());
            ps.setString(4, m.getStrength());
            ps.setBigDecimal(5, m.getUnitPrice());
            ps.setInt(6, m.getMinQuantity());
            ps.setBoolean(7, m.isPrescriptionRequired());
            ps.setBoolean(8, m.isActive());
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    public void update(Medicine m) {
        jdbc.update("UPDATE medicines SET name = ?, active_ingredient = ?, form = ?, strength = ?, "
                    + "unit_price = ?, min_quantity = ?, prescription_required = ?, active = ? WHERE id = ?",
                m.getName(), m.getActiveIngredient(), m.getForm(), m.getStrength(),
                m.getUnitPrice(), m.getMinQuantity(), m.isPrescriptionRequired(),
                m.isActive(), m.getId());
    }

    /** Лекарство с история не се трие физически, а се деактивира (пази целостта на справките). */
    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE medicines SET active = ? WHERE id = ?", active, id);
    }

    /** Екранира % и _ , за да не се превърне потребителският текст в заместващи символи. */
    static String safeLike(String term) {
        if (term == null) return "";
        return term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
