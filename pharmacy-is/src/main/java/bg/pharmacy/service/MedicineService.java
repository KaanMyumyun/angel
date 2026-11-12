package bg.pharmacy.service;

import bg.pharmacy.model.Batch;
import bg.pharmacy.model.Medicine;
import bg.pharmacy.model.MedicineStock;
import bg.pharmacy.repository.BatchRepository;
import bg.pharmacy.repository.MedicineRepository;
import bg.pharmacy.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicines;
    private final StockRepository stock;
    private final BatchRepository batches;

    public MedicineService(MedicineRepository medicines, StockRepository stock,
                           BatchRepository batches) {
        this.medicines = medicines;
        this.stock = stock;
        this.batches = batches;
    }

    @Transactional(readOnly = true)
    public List<Medicine> findAll() { return medicines.findAll(); }

    @Transactional(readOnly = true)
    public List<Medicine> findAllActive() { return medicines.findAllActive(); }

    @Transactional(readOnly = true)
    public Medicine requireMedicine(long id) {
        return medicines.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Лекарство с ID " + id + " не съществува."));
    }

    /** Търсене по име или активна съставка - използва се от полето за търсене на касата. */
    @Transactional(readOnly = true)
    public List<Medicine> search(String term) {
        if (term == null || term.isBlank()) return medicines.findAllActive();
        return medicines.search(term.trim());
    }

    @Transactional(readOnly = true)
    public List<MedicineStock> stockList(String term, Long supplierId) {
        return stock.findStock(term, supplierId);
    }

    @Transactional(readOnly = true)
    public MedicineStock requireStock(long medicineId) {
        return stock.findStockFor(medicineId).orElseThrow(
                () -> new IllegalArgumentException("Лекарство с ID " + medicineId + " не съществува."));
    }

    @Transactional(readOnly = true)
    public List<Batch> batchesOf(long medicineId) { return batches.findByMedicine(medicineId); }

    @Transactional
    public long create(Medicine m) { return medicines.insert(m); }

    @Transactional
    public void update(Medicine m) {
        requireMedicine(m.getId());
        medicines.update(m);
    }

    /** Не трием физически - лекарството има история на продажби и доставки. */
    @Transactional
    public void setActive(long id, boolean active) {
        requireMedicine(id);
        medicines.setActive(id, active);
    }
}
