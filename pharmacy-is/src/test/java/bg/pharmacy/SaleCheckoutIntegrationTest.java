package bg.pharmacy;

import bg.pharmacy.logic.InsufficientStockException;
import bg.pharmacy.model.Cart;
import bg.pharmacy.repository.StockMovementRepository;
import bg.pharmacy.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционни тестове на продажбата срещу истинска база (H2 в режим MySQL),
 * създадена от същия файл database/01_schema.sql.
 *
 * Проверява най-важния критерий от заданието: наличностите никога да не се разминават.
 */
@SpringBootTest
@ActiveProfiles("test")
class SaleCheckoutIntegrationTest {

    @Autowired private bg.pharmacy.service.SaleService saleService;
    @Autowired private StockMovementRepository movements;
    @Autowired private JdbcTemplate jdbc;

    private long medicineId;
    private long oldBatchId;    // срок след 30 дни - трябва да се продаде ПЪРВИ
    private long newBatchId;    // срок след 400 дни

    @BeforeEach
    void setUp() {
        TestData.clean(jdbc);
        TestData.insertUsers(jdbc);
        long supplierId = TestData.insertSupplier(jdbc, "Тестов доставчик");
        long deliveryId = TestData.insertDelivery(jdbc, supplierId);

        medicineId = TestData.insertMedicine(jdbc, "Аналгин 500 mg", "Метамизол натрий",
                new BigDecimal("3.20"), 20);

        LocalDate today = LocalDate.now();
        oldBatchId = TestData.insertBatch(jdbc, medicineId, deliveryId, "OLD",
                today.plusDays(30), 10, new BigDecimal("1.90"));
        newBatchId = TestData.insertBatch(jdbc, medicineId, deliveryId, "NEW",
                today.plusDays(400), 50, new BigDecimal("1.95"));
    }

    private int available(long batchId) {
        return jdbc.queryForObject(
                "SELECT quantity_available FROM batches WHERE id = ?", Integer.class, batchId);
    }

    @Test
    @DisplayName("Продажбата намалява наличността и записва продажба, артикули и движения")
    void checkoutReducesStockAndWritesAudit() {
        Cart cart = new Cart();
        cart.add(medicineId, "Аналгин 500 mg", new BigDecimal("3.20"), 4);

        long saleId = saleService.checkout(cart, TestData.PHARMACIST_ID);

        // от партидата с по-ранен срок (FEFO)
        assertEquals(6, available(oldBatchId));
        assertEquals(50, available(newBatchId));

        BigDecimal total = jdbc.queryForObject(
                "SELECT total FROM sales WHERE id = ?", BigDecimal.class, saleId);
        assertEquals(0, new BigDecimal("12.80").compareTo(total), "4 x 3.20 = 12.80");

        Integer items = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sale_items WHERE sale_id = ?", Integer.class, saleId);
        assertEquals(1, items);

        Integer saleMoves = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movements WHERE reason = 'SALE' AND ref_id = ?",
                Integer.class, saleId);
        assertEquals(1, saleMoves);
    }

    @Test
    @DisplayName("Количество, надвишаващо една партида, се разпределя по две")
    void checkoutSplitsAcrossBatches() {
        Cart cart = new Cart();
        cart.add(medicineId, "Аналгин 500 mg", new BigDecimal("3.20"), 15);

        long saleId = saleService.checkout(cart, TestData.PHARMACIST_ID);

        assertEquals(0, available(oldBatchId), "старата партида се изчерпва изцяло");
        assertEquals(45, available(newBatchId), "остатъкът идва от новата");

        Integer items = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sale_items WHERE sale_id = ?", Integer.class, saleId);
        assertEquals(2, items, "два реда - по един за всяка партида");
    }

    @Test
    @DisplayName("Недостатъчна наличност -> нищо не се записва (пълен откат)")
    void insufficientStockRollsBackCompletely() {
        Cart cart = new Cart();
        cart.add(medicineId, "Аналгин 500 mg", new BigDecimal("3.20"), 999);

        assertThrows(InsufficientStockException.class,
                () -> saleService.checkout(cart, TestData.PHARMACIST_ID));

        assertEquals(10, available(oldBatchId), "наличността е непроменена");
        assertEquals(50, available(newBatchId));
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM sales", Integer.class));
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM sale_items", Integer.class));
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movements WHERE reason = 'SALE'", Integer.class));
    }

    @Test
    @DisplayName("Изтекла партида не се продава")
    void expiredBatchIsNotSold() {
        long supplierId = TestData.insertSupplier(jdbc, "Втори доставчик");
        long deliveryId = TestData.insertDelivery(jdbc, supplierId);
        long expiredMedicine = TestData.insertMedicine(jdbc, "Нурофен сироп", "Ибупрофен",
                new BigDecimal("9.80"), 5);
        long expiredBatch = TestData.insertBatch(jdbc, expiredMedicine, deliveryId, "EXP",
                LocalDate.now().minusDays(5), 40, new BigDecimal("6.00"));

        Cart cart = new Cart();
        cart.add(expiredMedicine, "Нурофен сироп", new BigDecimal("9.80"), 1);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> saleService.checkout(cart, TestData.PHARMACIST_ID));
        assertEquals(0, ex.getAvailable(), "изтеклите бройки не се броят за налични");
        assertEquals(40, available(expiredBatch), "изтеклата партида остава непокътната");
    }

    @Test
    @DisplayName("Сумата на движенията съвпада с реалната наличност (сверка)")
    void movementsReconcileWithStock() {
        Cart cart = new Cart();
        cart.add(medicineId, "Аналгин 500 mg", new BigDecimal("3.20"), 7);
        saleService.checkout(cart, TestData.PHARMACIST_ID);

        int movementSum = movements.movementSumFor(medicineId);
        Integer actualStock = jdbc.queryForObject(
                "SELECT SUM(quantity_available) FROM batches WHERE medicine_id = ?",
                Integer.class, medicineId);

        assertEquals(movementSum, actualStock,
                "сборът на движенията трябва да е равен на наличността");
        assertEquals(53, movementSum, "60 доставени - 7 продадени");
    }

    @Test
    @DisplayName("Празна кошница не създава продажба")
    void emptyCartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> saleService.checkout(new Cart(), TestData.PHARMACIST_ID));
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM sales", Integer.class));
    }

    @Test
    @DisplayName("Цената се взема от базата, а не от подадената в кошницата")
    void priceComesFromDatabaseNotFromCart() {
        Cart cart = new Cart();
        // Опит за "намаление" чрез подправена цена в кошницата:
        cart.add(medicineId, "Аналгин 500 mg", new BigDecimal("0.01"), 2);

        long saleId = saleService.checkout(cart, TestData.PHARMACIST_ID);

        BigDecimal total = jdbc.queryForObject(
                "SELECT total FROM sales WHERE id = ?", BigDecimal.class, saleId);
        assertEquals(0, new BigDecimal("6.40").compareTo(total),
                "сумата се изчислява по цената в базата (2 x 3.20)");
    }
}
