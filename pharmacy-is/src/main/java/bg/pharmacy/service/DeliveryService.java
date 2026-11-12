package bg.pharmacy.service;

import bg.pharmacy.model.Batch;
import bg.pharmacy.model.Delivery;
import bg.pharmacy.model.DeliveryLine;
import bg.pharmacy.repository.BatchRepository;
import bg.pharmacy.repository.DeliveryRepository;
import bg.pharmacy.repository.MedicineRepository;
import bg.pharmacy.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Доставки: всеки доставен ред създава нова партида и увеличава наличността. */
@Service
public class DeliveryService {

    private final DeliveryRepository deliveries;
    private final BatchRepository batches;
    private final MedicineRepository medicines;
    private final StockMovementRepository movements;

    public DeliveryService(DeliveryRepository deliveries, BatchRepository batches,
                           MedicineRepository medicines, StockMovementRepository movements) {
        this.deliveries = deliveries;
        this.batches = batches;
        this.medicines = medicines;
        this.movements = movements;
    }

    /** Записва доставката заедно с партидите ѝ в една транзакция. */
    @Transactional
    public long receive(Delivery delivery, long userId) {
        if (delivery.getLines() == null || delivery.getLines().isEmpty()) {
            throw new IllegalArgumentException("Доставката трябва да съдържа поне един артикул.");
        }
        LocalDate today = LocalDate.now();

        for (DeliveryLine line : delivery.getLines()) {
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                throw new IllegalArgumentException("Количеството трябва да е поне 1 брой.");
            }
            if (line.getExpiryDate() == null || line.getExpiryDate().isBefore(today)) {
                throw new IllegalArgumentException(
                        "Не може да се приеме стока с изтекъл срок на годност.");
            }
            medicines.findById(line.getMedicineId()).orElseThrow(
                    () -> new IllegalArgumentException("Избрано е несъществуващо лекарство."));
        }

        delivery.setUserId(userId);
        long deliveryId = deliveries.insert(delivery);

        for (DeliveryLine line : delivery.getLines()) {
            Batch b = new Batch();
            b.setMedicineId(line.getMedicineId());
            b.setDeliveryId(deliveryId);
            b.setBatchNo(line.getBatchNo());
            b.setExpiryDate(line.getExpiryDate());
            b.setQuantityReceived(line.getQuantity());
            b.setQuantityAvailable(line.getQuantity());   // при доставка всичко е налично
            b.setUnitCost(line.getUnitCost());

            long batchId = batches.insert(b);
            movements.record(line.getMedicineId(), batchId, line.getQuantity(),
                             StockMovementRepository.Reason.DELIVERY, deliveryId, userId);
        }
        return deliveryId;
    }

    @Transactional(readOnly = true)
    public List<Delivery> recent(int limit) { return deliveries.findRecent(limit); }

    @Transactional(readOnly = true)
    public Delivery requireDelivery(long id) {
        Delivery d = deliveries.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Доставка с ID " + id + " не съществува."));
        d.setLines(toLines(deliveries.findBatches(id)));
        return d;
    }

    private static List<DeliveryLine> toLines(List<Batch> batchList) {
        return batchList.stream().map(b -> {
            DeliveryLine l = new DeliveryLine();
            l.setMedicineId(b.getMedicineId());
            l.setMedicineName(b.getMedicineName());
            l.setBatchNo(b.getBatchNo());
            l.setExpiryDate(b.getExpiryDate());
            l.setQuantity(b.getQuantityReceived());
            l.setUnitCost(b.getUnitCost());
            return l;
        }).toList();
    }
}
