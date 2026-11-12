package bg.pharmacy.service;

import bg.pharmacy.logic.BatchAllocation;
import bg.pharmacy.logic.FefoAllocator;
import bg.pharmacy.model.*;
import bg.pharmacy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Продажби.
 *
 * Цялата продажба минава в ЕДНА транзакция: или се записва изцяло
 * (продажба + артикули + намалени наличности + движения), или нищо.
 * Така наличностите не могат да се разминат при грешка по средата.
 */
@Service
public class SaleService {

    private final MedicineRepository medicines;
    private final BatchRepository batches;
    private final SaleRepository sales;
    private final StockMovementRepository movements;

    public SaleService(MedicineRepository medicines, BatchRepository batches,
                       SaleRepository sales, StockMovementRepository movements) {
        this.medicines = medicines;
        this.batches = batches;
        this.sales = sales;
        this.movements = movements;
    }

    /**
     * Приключва продажбата на съдържанието на кошницата.
     *
     * @return ID на записаната продажба
     * @throws bg.pharmacy.logic.InsufficientStockException при недостатъчна наличност
     * @throws IllegalStateException ако наличността се промени между проверката и записа
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long checkout(Cart cart, long userId) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Кошницата е празна.");
        }

        LocalDate today = LocalDate.now();

        // 1. Обединяваме евентуални повтарящи се редове за едно и също лекарство.
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (CartLine line : cart.getLines()) {
            wanted.merge(line.getMedicineId(), line.getQuantity(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<PlannedItem> planned = new ArrayList<>();

        // 2. Заключваме партидите и разпределяме по FEFO.
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            long medicineId = entry.getKey();
            int quantity = entry.getValue();

            Medicine medicine = medicines.findById(medicineId).orElseThrow(
                    () -> new IllegalArgumentException("Лекарството не съществува (ID " + medicineId + ")."));
            if (!medicine.isActive()) {
                throw new IllegalArgumentException(
                        "\"" + medicine.getName() + "\" е извадено от продажба.");
            }

            // Цената се взема от базата, а не от екрана - клиентът не определя сумата.
            BigDecimal unitPrice = medicine.getUnitPrice();

            List<Batch> locked = batches.findForUpdate(medicineId);
            List<BatchAllocation> allocations =
                    FefoAllocator.allocate(locked, quantity, today, medicine.getName());

            for (BatchAllocation a : allocations) {
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(a.getQuantity()))
                                                .setScale(2, RoundingMode.HALF_UP);
                planned.add(new PlannedItem(medicineId, a.getBatchId(), a.getQuantity(),
                                            unitPrice, lineTotal));
                total = total.add(lineTotal);
            }
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        // 3. Записваме продажбата и намаляваме наличностите.
        long saleId = sales.insertSale(userId, total);

        for (PlannedItem item : planned) {
            int affected = batches.decrease(item.batchId, item.quantity);
            if (affected != 1) {
                // Наличността е била променена междувременно -> цялата транзакция пада.
                throw new IllegalStateException(
                        "Наличността се промени по време на продажбата. Опитайте отново.");
            }
            sales.insertItem(saleId, item.medicineId, item.batchId,
                             item.quantity, item.unitPrice, item.lineTotal);
            movements.record(item.medicineId, item.batchId, -item.quantity,
                             StockMovementRepository.Reason.SALE, saleId, userId);
        }
        return saleId;
    }

    @Transactional(readOnly = true)
    public List<Sale> recentSales(int limit) { return sales.findRecent(limit); }

    @Transactional(readOnly = true)
    public Sale requireSale(long id) {
        return sales.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Продажба с ID " + id + " не съществува."));
    }

    /** Вътрешен носител: какво ще се запише, преди да се пипне базата. */
    private static final class PlannedItem {
        final long medicineId;
        final long batchId;
        final int quantity;
        final BigDecimal unitPrice;
        final BigDecimal lineTotal;

        PlannedItem(long medicineId, long batchId, int quantity,
                    BigDecimal unitPrice, BigDecimal lineTotal) {
            this.medicineId = medicineId;
            this.batchId = batchId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }
    }
}
