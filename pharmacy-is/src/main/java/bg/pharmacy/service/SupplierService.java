package bg.pharmacy.service;

import bg.pharmacy.model.Supplier;
import bg.pharmacy.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository suppliers;

    public SupplierService(SupplierRepository suppliers) { this.suppliers = suppliers; }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() { return suppliers.findAll(); }

    @Transactional(readOnly = true)
    public List<Supplier> findAllActive() { return suppliers.findAllActive(); }

    @Transactional(readOnly = true)
    public Supplier requireSupplier(long id) {
        return suppliers.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Доставчик с ID " + id + " не съществува."));
    }

    @Transactional
    public long create(Supplier s) {
        if (suppliers.nameExists(s.getName(), null)) {
            throw new IllegalArgumentException("Вече съществува доставчик с име \"" + s.getName() + "\".");
        }
        return suppliers.insert(s);
    }

    @Transactional
    public void update(Supplier s) {
        requireSupplier(s.getId());
        if (suppliers.nameExists(s.getName(), s.getId())) {
            throw new IllegalArgumentException("Вече съществува друг доставчик с име \"" + s.getName() + "\".");
        }
        suppliers.update(s);
    }
}
