package bg.pharmacy.web;

import bg.pharmacy.service.AlertService;
import bg.pharmacy.service.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Добавя броя предупреждения и името на потребителя във всяка страница (за лентата горе). */
@ControllerAdvice(assignableTypes = {
        HomeController.class, SaleController.class, MedicineController.class,
        SupplierController.class, DeliveryController.class, StockController.class,
        ReportController.class})
public class GlobalModelAdvice {

    private final AlertService alerts;

    public GlobalModelAdvice(AlertService alerts) { this.alerts = alerts; }

    @ModelAttribute
    public void common(Model model, @AuthenticationPrincipal AppUserPrincipal user) {
        if (user == null) return;
        model.addAttribute("currentUser", user);
        model.addAttribute("alertCount", alerts.totalCount());
    }
}
