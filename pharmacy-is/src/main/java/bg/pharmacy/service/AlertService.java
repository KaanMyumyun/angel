package bg.pharmacy.service;

import bg.pharmacy.logic.ExpiryPolicy;
import bg.pharmacy.model.Alert;
import bg.pharmacy.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Предупрежденията, които се показват при вход в системата и в секция "Склад":
 * ниска наличност, изтичащ срок и вече изтекла стока.
 */
@Service
public class AlertService {

    private final StockRepository stock;
    private final ExpiryPolicy expiryPolicy;

    public AlertService(StockRepository stock, ExpiryPolicy expiryPolicy) {
        this.stock = stock;
        this.expiryPolicy = expiryPolicy;
    }

    public int getWarningDays() { return expiryPolicy.getWarningDays(); }

    @Transactional(readOnly = true)
    public List<Alert> lowStock() { return stock.findLowStock(); }

    @Transactional(readOnly = true)
    public List<Alert> expiringSoon() { return stock.findExpiringSoon(expiryPolicy.getWarningDays()); }

    @Transactional(readOnly = true)
    public List<Alert> expired() { return stock.findExpired(); }

    /** Всички предупреждения наведнъж - първо изтеклите, после изтичащите, после ниската наличност. */
    @Transactional(readOnly = true)
    public List<Alert> all() {
        List<Alert> all = new ArrayList<>();
        all.addAll(expired());
        all.addAll(expiringSoon());
        all.addAll(lowStock());
        return all;
    }

    @Transactional(readOnly = true)
    public int totalCount() {
        return expired().size() + expiringSoon().size() + lowStock().size();
    }
}
