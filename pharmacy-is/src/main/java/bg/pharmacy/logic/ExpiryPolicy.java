package bg.pharmacy.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Правилото "скоро изтичащ срок". Прагът е настройваем през
 * application.properties (pharmacy.expiry-warning-days), по подразбиране 90 дни.
 */
public final class ExpiryPolicy {

    private final int warningDays;

    public ExpiryPolicy(int warningDays) {
        if (warningDays < 0) {
            throw new IllegalArgumentException("Прагът не може да е отрицателен.");
        }
        this.warningDays = warningDays;
    }

    public int getWarningDays() { return warningDays; }

    public boolean isExpired(LocalDate expiryDate, LocalDate today) {
        return expiryDate.isBefore(today);
    }

    /** Вярно само за още валидни партиди, чийто срок изтича в рамките на прага. */
    public boolean isExpiringSoon(LocalDate expiryDate, LocalDate today) {
        if (isExpired(expiryDate, today)) return false;
        return ChronoUnit.DAYS.between(today, expiryDate) <= warningDays;
    }
}
