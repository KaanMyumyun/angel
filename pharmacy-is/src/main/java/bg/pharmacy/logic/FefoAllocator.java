package bg.pharmacy.logic;

import bg.pharmacy.model.Batch;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * FEFO - First Expired, First Out.
 *
 * Разпределя исканото количество по партиди, започвайки от тази с най-ранен
 * срок на годност, за да не остава стока, която ще изтече в склада.
 * Изтеклите партиди се пропускат - те не могат да се продават.
 *
 * Класът е чист (няма база, няма време "отвън" - днешната дата се подава),
 * затова се тества директно с модулни тестове.
 */
public final class FefoAllocator {

    private FefoAllocator() { }

    /**
     * @param batches      партидите на едно лекарство (в произволен ред)
     * @param requested    искано количество, трябва да е > 0
     * @param today        днешна дата (подава се, за да е тестваемо)
     * @param medicineName име за съобщението при грешка
     * @return списък "партида -> количество", подредени по срок на годност
     * @throws InsufficientStockException ако неизтеклите партиди не стигат
     */
    public static List<BatchAllocation> allocate(List<Batch> batches, int requested,
                                                 LocalDate today, String medicineName) {
        if (requested <= 0) {
            throw new IllegalArgumentException("Количеството трябва да е положително число.");
        }

        List<Batch> usable = new ArrayList<>();
        for (Batch b : batches) {
            if (b.getQuantityAvailable() > 0 && !b.isExpired(today)) {
                usable.add(b);
            }
        }
        usable.sort(Comparator.comparing(Batch::getExpiryDate)
                              .thenComparing(b -> b.getId() == null ? 0L : b.getId()));

        int available = usable.stream().mapToInt(Batch::getQuantityAvailable).sum();
        if (available < requested) {
            throw new InsufficientStockException(medicineName, requested, available);
        }

        List<BatchAllocation> result = new ArrayList<>();
        int remaining = requested;
        for (Batch b : usable) {
            if (remaining == 0) break;
            int take = Math.min(remaining, b.getQuantityAvailable());
            result.add(new BatchAllocation(b.getId(), take));
            remaining -= take;
        }
        return result;
    }

    /** Колко може да се продаде в момента (без изтеклите партиди). */
    public static int sellableQuantity(List<Batch> batches, LocalDate today) {
        return batches.stream()
                      .filter(b -> !b.isExpired(today))
                      .mapToInt(Batch::getQuantityAvailable)
                      .sum();
    }
}
