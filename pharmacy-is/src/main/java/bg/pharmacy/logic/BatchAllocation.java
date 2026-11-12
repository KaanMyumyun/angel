package bg.pharmacy.logic;

/** Решение "вземи N броя от партида X". Резултат от FEFO разпределението. */
public class BatchAllocation {
    private final long batchId;
    private final int quantity;

    public BatchAllocation(long batchId, int quantity) {
        this.batchId = batchId;
        this.quantity = quantity;
    }

    public long getBatchId() { return batchId; }
    public int getQuantity() { return quantity; }

    @Override
    public String toString() { return "BatchAllocation{batch=" + batchId + ", qty=" + quantity + "}"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BatchAllocation)) return false;
        BatchAllocation that = (BatchAllocation) o;
        return batchId == that.batchId && quantity == that.quantity;
    }

    @Override
    public int hashCode() { return Long.hashCode(batchId) * 31 + quantity; }
}
