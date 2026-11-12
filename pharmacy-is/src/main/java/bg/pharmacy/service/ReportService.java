package bg.pharmacy.service;

import bg.pharmacy.model.ProfitSummary;
import bg.pharmacy.model.TopSellerRow;
import bg.pharmacy.model.TurnoverRow;
import bg.pharmacy.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Справки - достъпни само за роля MANAGER (виж SecurityConfig). */
@Service
public class ReportService {

    private final ReportRepository reports;

    public ReportService(ReportRepository reports) { this.reports = reports; }

    @Transactional(readOnly = true)
    public List<TurnoverRow> dailyTurnover(LocalDate from, LocalDate to) {
        return reports.dailyTurnover(from, to);
    }

    @Transactional(readOnly = true)
    public List<TopSellerRow> topSellers(LocalDate from, LocalDate to, int limit) {
        return reports.topSellers(from, to, limit);
    }

    @Transactional(readOnly = true)
    public ProfitSummary profit(LocalDate from, LocalDate to) { return reports.profit(from, to); }

    @Transactional(readOnly = true)
    public BigDecimal stockValue() { return reports.stockValue(); }
}
