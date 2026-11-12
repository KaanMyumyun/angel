package bg.pharmacy.web;

import bg.pharmacy.model.Alert;
import bg.pharmacy.service.AlertService;
import bg.pharmacy.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

/**
 * Начален екран. При вход веднага се показват предупрежденията за
 * ниска наличност и изтичащ срок - за да се реагира навреме.
 */
@Controller
public class HomeController {

    private final AlertService alerts;
    private final ReportService reports;

    public HomeController(AlertService alerts, ReportService reports) {
        this.alerts = alerts;
        this.reports = reports;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Alert> expired = alerts.expired();
        List<Alert> expiring = alerts.expiringSoon();
        List<Alert> low = alerts.lowStock();

        model.addAttribute("expired", expired);
        model.addAttribute("expiring", expiring);
        model.addAttribute("lowStock", low);
        model.addAttribute("warningDays", alerts.getWarningDays());
        model.addAttribute("hasAlerts", !expired.isEmpty() || !expiring.isEmpty() || !low.isEmpty());

        LocalDate today = LocalDate.now();
        model.addAttribute("todayTurnover", reports.dailyTurnover(today, today));
        return "index";
    }
}
