package bg.pharmacy.web;

import bg.pharmacy.model.Supplier;
import bg.pharmacy.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService suppliers;

    public SupplierController(SupplierService suppliers) { this.suppliers = suppliers; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", suppliers.findAll());
        return "suppliers";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "supplier-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("supplier", suppliers.requireSupplier(id));
        return "supplier-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("supplier") Supplier supplier,
                       BindingResult binding, RedirectAttributes flash) {
        if (binding.hasErrors()) return "supplier-form";
        try {
            if (supplier.getId() == null) {
                suppliers.create(supplier);
                flash.addFlashAttribute("success", "Доставчикът е добавен.");
            } else {
                suppliers.update(supplier);
                flash.addFlashAttribute("success", "Промените са записани.");
            }
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("name", "duplicate", ex.getMessage());
            return "supplier-form";
        }
        return "redirect:/suppliers";
    }
}
