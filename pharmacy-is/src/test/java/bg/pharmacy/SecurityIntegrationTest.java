package bg.pharmacy;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Тестове на удостоверяването, нивата на достъп и защитите (CSRF, XSS, SQL Injection). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    private long medicineId;

    @BeforeEach
    void setUp() {
        TestData.clean(jdbc);
        TestData.insertUsers(jdbc);
        long supplierId = TestData.insertSupplier(jdbc, "Доставчик");
        long deliveryId = TestData.insertDelivery(jdbc, supplierId);
        medicineId = TestData.insertMedicine(jdbc, "Аналгин 500 mg", "Метамизол натрий",
                new BigDecimal("3.20"), 20);
        TestData.insertBatch(jdbc, medicineId, deliveryId, "B1",
                LocalDate.now().plusDays(200), 100, new BigDecimal("1.90"));
    }

    @Test
    @DisplayName("Без вход всяка страница пренасочва към формата за вход")
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/stock"))
           .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Вход с вярна парола успява, с грешна - не")
    void loginWorksWithCorrectPassword() throws Exception {
        mvc.perform(formLogin("/login").user("manager").password("manager123"))
           .andExpect(authenticated().withRoles("MANAGER"));

        mvc.perform(formLogin("/login").user("manager").password("грешна"))
           .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Фармацевтът НЯМА достъп до справките, управителят има")
    void reportsAreManagerOnly() throws Exception {
        mvc.perform(get("/reports").with(user(TestData.pharmacist())))
           .andExpect(status().isForbidden());

        mvc.perform(get("/reports").with(user(TestData.manager())))
           .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Фармацевтът не може да редактира каталога")
    void catalogEditingIsManagerOnly() throws Exception {
        mvc.perform(get("/medicines/new").with(user(TestData.pharmacist())))
           .andExpect(status().isForbidden());

        mvc.perform(get("/medicines/new").with(user(TestData.manager())))
           .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST без CSRF токен се отхвърля")
    void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/sales/add")
                        .param("medicineId", String.valueOf(medicineId))
                        .param("quantity", "1")
                        .with(user(TestData.pharmacist())))
           .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST с валиден CSRF токен се приема")
    void postWithCsrfIsAccepted() throws Exception {
        mvc.perform(post("/sales/add")
                        .param("medicineId", String.valueOf(medicineId))
                        .param("quantity", "1")
                        .with(user(TestData.pharmacist()))
                        .with(csrf()))
           .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Опит за SQL Injection в търсачката не поврежда базата")
    void searchIsSafeAgainstSqlInjection() throws Exception {
        String attack = "'; DROP TABLE medicines; --";

        mvc.perform(get("/stock").param("q", attack).with(user(TestData.pharmacist())))
           .andExpect(status().isOk());

        // Таблицата е непокътната - параметърът е третиран като текст, не като SQL.
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM medicines", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("Специалните знаци на LIKE се екранират, а не се тълкуват")
    void likeWildcardsAreEscaped() throws Exception {
        // "%" би върнал всичко, ако не се екранира
        mvc.perform(get("/stock").param("q", "%").with(user(TestData.pharmacist())))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("Аналгин"))));
    }

    @Test
    @DisplayName("Опасен текст в името се извежда екраниран (XSS защита)")
    void outputIsEscapedAgainstXss() throws Exception {
        jdbc.update("UPDATE medicines SET name = ? WHERE id = ?",
                "<script>alert('xss')</script>", medicineId);

        mvc.perform(get("/medicines").with(user(TestData.pharmacist())))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("<script>alert("))))
           .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    @DisplayName("Паролите се пазят само като BCrypt хеш")
    void passwordsAreStoredHashed() {
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE username = 'manager'", String.class);
        org.junit.jupiter.api.Assertions.assertTrue(hash.startsWith("$2a$"),
                "паролата трябва да е BCrypt хеш");
        org.junit.jupiter.api.Assertions.assertFalse(hash.contains("manager123"));
    }
}
