package bg.pharmacy.logic;

import bg.pharmacy.model.Batch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Модулни тестове на FEFO разпределението - нормални и гранични случаи. */
class FefoAllocatorTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);

    private static Batch batch(long id, String expiry, int available) {
        return new Batch(id, 1L, LocalDate.parse(expiry), available);
    }

    @Test
    @DisplayName("Взема се първо партидата с най-ранен срок на годност")
    void allocatesFromEarliestExpiryFirst() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2026-01-01", 50),
                batch(2, "2025-08-01", 30),   // най-ранен срок
                batch(3, "2025-12-01", 40));

        List<BatchAllocation> result = FefoAllocator.allocate(batches, 20, TODAY, "Аналгин");

        assertEquals(1, result.size());
        assertEquals(new BatchAllocation(2, 20), result.get(0));
    }

    @Test
    @DisplayName("Количество, по-голямо от една партида, се разпределя по няколко")
    void splitsAcrossBatchesInExpiryOrder() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2026-01-01", 50),
                batch(2, "2025-08-01", 30),
                batch(3, "2025-12-01", 40));

        List<BatchAllocation> result = FefoAllocator.allocate(batches, 85, TODAY, "Аналгин");

        assertEquals(Arrays.asList(
                new BatchAllocation(2, 30),   // 2025-08-01 -> изчерпана
                new BatchAllocation(3, 40),   // 2025-12-01 -> изчерпана
                new BatchAllocation(1, 15)),  // 2026-01-01 -> частично
                result);
        assertEquals(85, result.stream().mapToInt(BatchAllocation::getQuantity).sum());
    }

    @Test
    @DisplayName("Изтекли партиди не се продават, дори да имат наличност")
    void skipsExpiredBatches() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2025-05-31", 100),  // изтекла вчера
                batch(2, "2025-09-01", 10));

        List<BatchAllocation> result = FefoAllocator.allocate(batches, 10, TODAY, "Нурофен");

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getBatchId());
    }

    @Test
    @DisplayName("Партида, изтичаща днес, все още е валидна")
    void batchExpiringTodayIsStillUsable() {
        List<Batch> batches = Collections.singletonList(batch(1, "2025-06-01", 5));

        List<BatchAllocation> result = FefoAllocator.allocate(batches, 5, TODAY, "Аспирин");

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getQuantity());
    }

    @Test
    @DisplayName("Недостатъчна наличност -> изключение със сравними количества")
    void throwsWhenNotEnoughStock() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2025-05-01", 100),  // изтекла - не се брои
                batch(2, "2025-09-01", 7));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> FefoAllocator.allocate(batches, 10, TODAY, "Амоксицилин"));

        assertEquals(10, ex.getRequested());
        assertEquals(7, ex.getAvailable());
        assertTrue(ex.getMessage().contains("Амоксицилин"));
    }

    @Test
    @DisplayName("Празен склад -> изключение, а не празно разпределение")
    void throwsOnEmptyStock() {
        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> FefoAllocator.allocate(Collections.emptyList(), 1, TODAY, "Витамин C"));
        assertEquals(0, ex.getAvailable());
    }

    @Test
    @DisplayName("Партиди с нулева наличност се пропускат")
    void skipsEmptyBatches() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2025-07-01", 0),
                batch(2, "2025-09-01", 3));

        List<BatchAllocation> result = FefoAllocator.allocate(batches, 3, TODAY, "АЦЦ");

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getBatchId());
    }

    @Test
    @DisplayName("Нулево или отрицателно количество е невалидно")
    void rejectsNonPositiveQuantity() {
        List<Batch> batches = Collections.singletonList(batch(1, "2026-01-01", 10));
        assertThrows(IllegalArgumentException.class,
                () -> FefoAllocator.allocate(batches, 0, TODAY, "Аналгин"));
        assertThrows(IllegalArgumentException.class,
                () -> FefoAllocator.allocate(batches, -5, TODAY, "Аналгин"));
    }

    @Test
    @DisplayName("Точно изчерпване на цялата налична стока")
    void allocatesExactlyAllAvailable() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2025-07-01", 10),
                batch(2, "2025-08-01", 5));

        List<BatchAllocation> result = FefoAllocator.allocate(batches, 15, TODAY, "Аналгин");

        assertEquals(15, result.stream().mapToInt(BatchAllocation::getQuantity).sum());
    }

    @Test
    @DisplayName("sellableQuantity не брои изтеклите партиди")
    void sellableQuantityExcludesExpired() {
        List<Batch> batches = Arrays.asList(
                batch(1, "2025-05-01", 100),
                batch(2, "2025-09-01", 7),
                batch(3, "2026-01-01", 3));

        assertEquals(10, FefoAllocator.sellableQuantity(batches, TODAY));
    }
}
