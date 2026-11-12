package bg.pharmacy.web;

import bg.pharmacy.model.Delivery;
import bg.pharmacy.model.DeliveryLine;
import bg.pharmacy.service.AppUserPrincipal;
import bg.pharmacy.service.DeliveryService;
import bg.pharmacy.service.MedicineService;
import bg.pharmacy.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Приемане на доставки - увеличава наличностите чрез нови партиди. */
@Controller
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveries;
    private final SupplierService suppliers;
    private final MedicineService medicines;

    public DeliveryController(DeliveryService deliveries, SupplierService suppliers,
                              MedicineService medicines) {
        this.deliveries = deliveries;
        this.suppliers = suppliers;
        this.medicines = medicines;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("deliveries", deliveries.recent(100));
        return "deliveries";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("delivery", deliveries.requireDelivery(id));
        return "delivery-details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Delivery delivery = new Delivery();
        delivery.getLines().add(new DeliveryLine());   // един празен ред за начало
        model.addAttribute("delivery", delivery);
        addLookups(model);
        return "delivery-form";
    }

    /** Добавя още един празен ред, без да записва (бутон "Още един артикул"). */
    @PostMapping("/add-line")
    public String addLine(@ModelAttribute("delivery") Delivery delivery, Model model) {
        delivery.getLines().add(new DeliveryLine());
        addLookups(model);
        return "delivery-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("delivery") Delivery delivery,
                       BindingResult binding, Model model,
                       @AuthenticationPrincipal AppUserPrincipal user,
                       RedirectAttributes flash) {
        if (binding.hasErrors()) {
            addLookups(model);
            return "delivery-form";
        }
        try {
            long id = deliveries.receive(delivery, user.getId());
            flash.addFlashAttribute("success",
                    "Доставката е заприходена - наличностите са увеличени.");
            return "redirect:/deliveries/" + id;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            addLookups(model);
            return "delivery-form";
        }
    }

    private void addLookups(Model model) {
        model.addAttribute("suppliers", suppliers.findAllActive());
        model.addAttribute("medicines", medicines.findAllActive());
    }
}
