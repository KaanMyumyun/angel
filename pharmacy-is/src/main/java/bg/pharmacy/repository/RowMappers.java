package bg.pharmacy.repository;

import bg.pharmacy.model.*;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Общи RowMapper-и, за да не се дублира превръщането ResultSet -> обект. */
public final class RowMappers {

    private RowMappers() { }

    public static final RowMapper<Medicine> MEDICINE = (rs, i) -> medicine(rs);

    public static Medicine medicine(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getLong("id"));
        m.setName(rs.getString("name"));
        m.setActiveIngredient(rs.getString("active_ingredient"));
        m.setForm(rs.getString("form"));
        m.setStrength(rs.getString("strength"));
        m.setUnitPrice(rs.getBigDecimal("unit_price"));
        m.setMinQuantity(rs.getInt("min_quantity"));
        m.setPrescriptionRequired(rs.getBoolean("prescription_required"));
        m.setActive(rs.getBoolean("active"));
        return m;
    }

    public static final RowMapper<Supplier> SUPPLIER = (rs, i) -> {
        Supplier s = new Supplier();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        s.setEik(rs.getString("eik"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setActive(rs.getBoolean("active"));
        return s;
    };

    public static final RowMapper<User> USER = (rs, i) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        return u;
    };

    public static final RowMapper<Batch> BATCH = (rs, i) -> {
        Batch b = new Batch();
        b.setId(rs.getLong("id"));
        b.setMedicineId(rs.getLong("medicine_id"));
        b.setDeliveryId(rs.getLong("delivery_id"));
        b.setBatchNo(rs.getString("batch_no"));
        b.setExpiryDate(rs.getDate("expiry_date").toLocalDate());
        b.setQuantityReceived(rs.getInt("quantity_received"));
        b.setQuantityAvailable(rs.getInt("quantity_available"));
        b.setUnitCost(rs.getBigDecimal("unit_cost"));
        if (hasColumn(rs, "medicine_name")) {
            b.setMedicineName(rs.getString("medicine_name"));
        }
        return b;
    };

    public static final RowMapper<Delivery> DELIVERY = (rs, i) -> {
        Delivery d = new Delivery();
        d.setId(rs.getLong("id"));
        d.setSupplierId(rs.getLong("supplier_id"));
        d.setDeliveryDate(rs.getDate("delivery_date").toLocalDate());
        d.setDocumentNo(rs.getString("document_no"));
        d.setNote(rs.getString("note"));
        d.setUserId(rs.getLong("user_id"));
        if (hasColumn(rs, "supplier_name")) d.setSupplierName(rs.getString("supplier_name"));
        if (hasColumn(rs, "user_name"))     d.setUserName(rs.getString("user_name"));
        return d;
    };

    public static final RowMapper<Sale> SALE = (rs, i) -> {
        Sale s = new Sale();
        s.setId(rs.getLong("id"));
        Timestamp ts = rs.getTimestamp("sold_at");
        s.setSoldAt(ts == null ? null : ts.toLocalDateTime());
        s.setUserId(rs.getLong("user_id"));
        s.setTotal(rs.getBigDecimal("total"));
        if (hasColumn(rs, "user_name")) s.setUserName(rs.getString("user_name"));
        return s;
    };

    public static final RowMapper<SaleItem> SALE_ITEM = (rs, i) -> {
        SaleItem it = new SaleItem();
        it.setId(rs.getLong("id"));
        it.setSaleId(rs.getLong("sale_id"));
        it.setMedicineId(rs.getLong("medicine_id"));
        it.setBatchId(rs.getLong("batch_id"));
        it.setQuantity(rs.getInt("quantity"));
        it.setUnitPrice(rs.getBigDecimal("unit_price"));
        it.setLineTotal(rs.getBigDecimal("line_total"));
        if (hasColumn(rs, "medicine_name")) it.setMedicineName(rs.getString("medicine_name"));
        if (hasColumn(rs, "batch_no"))      it.setBatchNo(rs.getString("batch_no"));
        return it;
    };

    public static final RowMapper<MedicineStock> MEDICINE_STOCK = (rs, i) -> {
        MedicineStock st = new MedicineStock();
        st.setMedicine(medicine(rs));
        st.setSellableQuantity(rs.getInt("sellable_quantity"));
        st.setExpiredQuantity(rs.getInt("expired_quantity"));
        Date nearest = rs.getDate("nearest_expiry");
        st.setNearestExpiry(nearest == null ? null : nearest.toLocalDate());
        return st;
    };

    private static boolean hasColumn(ResultSet rs, String column) throws SQLException {
        java.sql.ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (column.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }
}
