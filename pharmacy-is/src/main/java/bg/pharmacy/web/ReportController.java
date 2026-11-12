package bg.pharmacy.web;

import bg.pharmacy.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** Справки - само за роля MANAGER (ограничението е в SecurityConfig). */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping
    public String reports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate today = LocalDate.now();
        LocalDate start = (from != null) ? from : today.minusDays(29);
        LocalDate end   = (to   != null) ? to   : today;
        if (end.isBefore(start)) {
            LocalDate swap = start; start = end; end = swap;
        }

        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("turnover", reports.dailyTurnover(start, end));
        model.addAttribute("topSellers", reports.topSellers(start, end, 10));
        model.addAttribute("profit", reports.profit(start, end));
        model.addAttribute("stockValue", reports.stockValue());
        return "reports";
    }
}
