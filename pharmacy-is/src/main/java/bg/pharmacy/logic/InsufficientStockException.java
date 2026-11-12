package bg.pharmacy.logic;

/** Хвърля се, когато исканото количество надвишава наличното (без изтеклите партиди). */
public class InsufficientStockException extends RuntimeException {
    private final String medicineName;
    private final int requested;
    private final int available;

    public InsufficientStockException(String medicineName, int requested, int available) {
        super("Недостатъчна наличност за \"" + medicineName + "\": искани " + requested
              + " бр., налични " + available + " бр.");
        this.medicineName = medicineName;
        this.requested = requested;
        this.available = available;
    }

    public String getMedicineName() { return medicineName; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
