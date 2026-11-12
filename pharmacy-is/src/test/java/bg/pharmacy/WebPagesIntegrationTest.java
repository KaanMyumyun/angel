package bg.pharmacy;

import bg.pharmacy.model.Cart;
import bg.pharmacy.service.SaleService;
import bg.pharmacy.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Проверява, че ВСЯКА страница се изобразява без грешка в шаблона.
 * Така грешка в Thymeleaf израз се хваща от тестовете, а не от потребителя.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPagesIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SaleService saleService;

    private long medicineId;
    private long deliveryId;
    private long saleId;

    @BeforeEach
    void setUp() {
        TestData.clean(jdbc);
        TestData.insertUsers(jdbc);
        long supplierId = TestData.insertSupplier(jdbc, "Софарма Трейдинг АД");
        deliveryId = TestData.insertDelivery(jdbc, supplierId);
        medicineId = TestData.insertMedicine(jdbc, "Аналгин 500 mg", "Метамизол натрий",
                new BigDecimal("3.20"), 20);

        LocalDate today = LocalDate.now();
        // партида с изтичащ срок -> задейства предупреждението на началния екран
        TestData.insertBatch(jdbc, medicineId, deliveryId, "SOON",
                today.plusDays(15), 30, new BigDecimal("1.90"));
        // вече изтекла партида -> задейства второто предупреждение
        TestData.insertBatch(jdbc, medicineId, deliveryId, "OLD",
                today.minusDays(3), 5, new BigDecimal("1.85"));

        Cart cart = new Cart();
        cart.add(medicineId, "Аналгин 500 mg", new BigDecimal("3.20"), 2);
        saleId = saleService.checkout(cart, TestData.PHARMACIST_ID);
    }

    @Test
    @DisplayName("Страницата за вход се отваря без удостоверяване")
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login"))
           .andExpect(status().isOk())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Аптека")));
    }

    @Test
    @DisplayName("Начален екран показва предупрежденията")
    void dashboardRenders() throws Exception {
        mvc.perform(get("/").with(user(TestData.manager())))
           .andExpect(status().isOk())
           .andExpect(view().name("index"))
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Изтекъл срок")));
    }

    @Test
    @DisplayName("Каса - празна и с търсене")
    void posRenders() throws Exception {
        mvc.perform(get("/sales").with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("sales"));

        mvc.perform(get("/sales").param("q", "метамизол").with(user(TestData.pharmacist())))
           .andExpect(status().isOk())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Аналгин")));
    }

    @Test
    @DisplayName("Склад, каталог и детайли на лекарство")
    void stockAndCatalogRender() throws Exception {
        mvc.perform(get("/stock").with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("stock"));

        mvc.perform(get("/stock").param("q", "аналгин").with(user(TestData.pharmacist())))
           .andExpect(status().isOk());

        mvc.perform(get("/medicines").with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("medicines"));

        mvc.perform(get("/medicines/" + medicineId).with(user(TestData.manager())))
           .andExpect(status().isOk()).andExpect(view().name("medicine-details"));
    }

    @Test
    @DisplayName("Формите за лекарство и доставчик се отварят")
    void formsRender() throws Exception {
        mvc.perform(get("/medicines/new").with(user(TestData.manager())))
           .andExpect(status().isOk()).andExpect(view().name("medicine-form"));

        mvc.perform(get("/medicines/" + medicineId + "/edit").with(user(TestData.manager())))
           .andExpect(status().isOk()).andExpect(view().name("medicine-form"));

        mvc.perform(get("/suppliers").with(user(TestData.manager())))
           .andExpect(status().isOk()).andExpect(view().name("suppliers"));

        mvc.perform(get("/suppliers/new").with(user(TestData.manager())))
           .andExpect(status().isOk()).andExpect(view().name("supplier-form"));
    }

    @Test
    @DisplayName("Доставки - списък, форма и детайли")
    void deliveryPagesRender() throws Exception {
        mvc.perform(get("/deliveries").with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("deliveries"));

        mvc.perform(get("/deliveries/new").with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("delivery-form"));

        mvc.perform(get("/deliveries/" + deliveryId).with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("delivery-details"));
    }

    @Test
    @DisplayName("Продажби - история и касова бележка")
    void salesPagesRender() throws Exception {
        mvc.perform(get("/sales/history").with(user(TestData.pharmacist())))
           .andExpect(status().isOk()).andExpect(view().name("sales-history"));

        mvc.perform(get("/sales/receipt/" + saleId).with(user(TestData.pharmacist())))
           .andExpect(status().isOk())
           .andExpect(view().name("receipt"))
           .andExpect(content().string(org.hamcrest.Matchers.containsString("6.40")));
    }

    @Test
    @DisplayName("Справките се отварят за управителя")
    void reportsRenderForManager() throws Exception {
        mvc.perform(get("/reports").with(user(TestData.manager())))
           .andExpect(status().isOk())
           .andExpect(view().name("reports"))
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Най-продавани")));
    }
}
