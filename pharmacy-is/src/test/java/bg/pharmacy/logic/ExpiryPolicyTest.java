package bg.pharmacy.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExpiryPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);
    private final ExpiryPolicy policy = new ExpiryPolicy(90);

    @Test
    @DisplayName("Вчерашен срок = изтекъл")
    void detectsExpired() {
        assertTrue(policy.isExpired(LocalDate.of(2025, 5, 31), TODAY));
    }

    @Test
    @DisplayName("Днешен срок още не е изтекъл")
    void todayIsNotExpired() {
        assertFalse(policy.isExpired(TODAY, TODAY));
    }

    @Test
    @DisplayName("Срок в рамките на прага -> предупреждение")
    void warnsWithinThreshold() {
        assertTrue(policy.isExpiringSoon(LocalDate.of(2025, 7, 15), TODAY));
    }

    @Test
    @DisplayName("Точно на 90-ия ден все още предупреждава, на 91-ия - не")
    void thresholdBoundary() {
        assertTrue(policy.isExpiringSoon(TODAY.plusDays(90), TODAY));
        assertFalse(policy.isExpiringSoon(TODAY.plusDays(91), TODAY));
    }

    @Test
    @DisplayName("Вече изтекла партида не се брои за 'изтичаща скоро'")
    void expiredIsNotExpiringSoon() {
        assertFalse(policy.isExpiringSoon(LocalDate.of(2025, 1, 1), TODAY));
    }

    @Test
    @DisplayName("Отрицателен праг е невалиден")
    void rejectsNegativeThreshold() {
        assertThrows(IllegalArgumentException.class, () -> new ExpiryPolicy(-1));
    }
}
