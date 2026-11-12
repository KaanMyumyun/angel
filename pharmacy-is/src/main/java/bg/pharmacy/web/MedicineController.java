package bg.pharmacy.web;

import bg.pharmacy.model.Medicine;
import bg.pharmacy.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Каталог на лекарствата (CRUD). Редактирането е право на управителя. */
@Controller
@RequestMapping("/medicines")
public class MedicineController {

    private static final String[] FORMS = {
            "таблетки", "капсули", "сироп", "капки", "ампули", "крем",
            "мехлем", "спрей", "ефервесцентни таблетки", "прах", "супозитории"
    };

    private final MedicineService medicines;

    public MedicineController(MedicineService medicines) { this.medicines = medicines; }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query, Model model) {
        model.addAttribute("stockList", medicines.stockList(query, null));
        model.addAttribute("q", query);
        return "medicines";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model) {
        model.addAttribute("stock", medicines.requireStock(id));
        model.addAttribute("batches", medicines.batchesOf(id));
        return "medicine-details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        model.addAttribute("forms", FORMS);
        return "medicine-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("medicine", medicines.requireMedicine(id));
        model.addAttribute("forms", FORMS);
        return "medicine-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("medicine") Medicine medicine,
                       BindingResult binding, Model model, RedirectAttributes flash) {
        if (binding.hasErrors()) {
            model.addAttribute("forms", FORMS);
            return "medicine-form";
        }
        if (medicine.getId() == null) {
            long id = medicines.create(medicine);
            flash.addFlashAttribute("success", "Лекарството е добавено в каталога.");
            return "redirect:/medicines/" + id;
        }
        medicines.update(medicine);
        flash.addFlashAttribute("success", "Промените са записани.");
        return "redirect:/medicines/" + medicine.getId();
    }

    @PostMapping("/{id}/active")
    public String toggleActive(@PathVariable long id, @RequestParam boolean active,
                               RedirectAttributes flash) {
        medicines.setActive(id, active);
        flash.addFlashAttribute("success",
                active ? "Лекарството е върнато в продажба." : "Лекарството е извадено от продажба.");
        return "redirect:/medicines/" + id;
    }
}
