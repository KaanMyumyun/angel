package bg.pharmacy.web;

import bg.pharmacy.logic.InsufficientStockException;
import bg.pharmacy.model.Cart;
import bg.pharmacy.model.Medicine;
import bg.pharmacy.service.AppUserPrincipal;
import bg.pharmacy.service.MedicineService;
import bg.pharmacy.service.SaleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Касата: търсене, кошница и приключване на продажба. */
@Controller
@RequestMapping("/sales")
public class SaleController {

    private static final String CART_ATTRIBUTE = "cart";

    private final SaleService sales;
    private final MedicineService medicines;

    public SaleController(SaleService sales, MedicineService medicines) {
        this.sales = sales;
        this.medicines = medicines;
    }

    /** Кошницата живее в сесията на конкретния фармацевт. */
    private Cart cart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }

    @GetMapping
    public String pointOfSale(@RequestParam(name = "q", required = false) String query,
                              HttpSession session, Model model) {
        model.addAttribute("cart", cart(session));
        model.addAttribute("q", query);
        if (query != null && !query.isBlank()) {
            model.addAttribute("results", medicines.stockList(query, null));
        }
        return "sales";
    }

    @PostMapping("/add")
    public String add(@RequestParam long medicineId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(name = "q", required = false) String query,
                      HttpSession session, RedirectAttributes flash) {
        try {
            Medicine medicine = medicines.requireMedicine(medicineId);
            cart(session).add(medicineId, medicine.getName(), medicine.getUnitPrice(), quantity);
            flash.addFlashAttribute("success",
                    "Добавено: " + medicine.getName() + " x " + quantity);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToPos(query, flash);
    }

    @PostMapping("/update")
    public String update(@RequestParam long medicineId, @RequestParam int quantity,
                         HttpSession session) {
        cart(session).updateQuantity(medicineId, quantity);
        return "redirect:/sales";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam long medicineId, HttpSession session) {
        cart(session).remove(medicineId);
        return "redirect:/sales";
    }

    @PostMapping("/clear")
    public String clear(HttpSession session) {
        cart(session).clear();
        return "redirect:/sales";
    }

    /** Приключване: наличностите се намаляват в една транзакция (виж SaleService). */
    @PostMapping("/checkout")
    public String checkout(HttpSession session, @AuthenticationPrincipal AppUserPrincipal user,
                           RedirectAttributes flash) {
        Cart cart = cart(session);
        try {
            long saleId = sales.checkout(cart, user.getId());
            cart.clear();
            return "redirect:/sales/receipt/" + saleId;
        } catch (InsufficientStockException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/sales";
    }

    @GetMapping("/receipt/{id}")
    public String receipt(@PathVariable long id, Model model) {
        model.addAttribute("sale", sales.requireSale(id));
        return "receipt";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("sales", sales.recentSales(100));
        return "sales-history";
    }

    private String redirectToPos(String query, RedirectAttributes flash) {
        if (query != null && !query.isBlank()) {
            flash.addAttribute("q", query);   // запазва търсенето след добавяне
        }
        return "redirect:/sales";
    }
}
