package bg.pharmacy.web;

import bg.pharmacy.service.AlertService;
import bg.pharmacy.service.MedicineService;
import bg.pharmacy.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Склад. При влизане тук се показват предупрежденията за ниска наличност
 * и изтичащ срок, както изисква заданието.
 */
@Controller
@RequestMapping("/stock")
public class StockController {

    private final MedicineService medicines;
    private final SupplierService suppliers;
    private final AlertService alerts;

    public StockController(MedicineService medicines, SupplierService suppliers,
                           AlertService alerts) {
        this.medicines = medicines;
        this.suppliers = suppliers;
        this.alerts = alerts;
    }

    @GetMapping
    public String stock(@RequestParam(name = "q", required = false) String query,
                        @RequestParam(name = "supplierId", required = false) Long supplierId,
                        Model model) {
        model.addAttribute("stockList", medicines.stockList(query, supplierId));
        model.addAttribute("suppliers", suppliers.findAllActive());
        model.addAttribute("q", query);
        model.addAttribute("supplierId", supplierId);

        model.addAttribute("expired", alerts.expired());
        model.addAttribute("expiring", alerts.expiringSoon());
        model.addAttribute("lowStock", alerts.lowStock());
        model.addAttribute("warningDays", alerts.getWarningDays());
        return "stock";
    }
}
