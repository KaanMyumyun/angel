package bg.pharmacy;

import bg.pharmacy.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Пуска истински уеб сървър (Tomcat) и прави реални HTTP заявки.
 * Проверява, че приложението се вдига, схемата се създава автоматично
 * и статичните ресурси се отдават.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpSmokeTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestData.clean(jdbc);
        TestData.insertUsers(jdbc);
    }

    private String url(String path) { return "http://localhost:" + port + path; }

    @Test
    @DisplayName("Приложението се вдига и отдава страницата за вход")
    void loginPageIsServed() {
        ResponseEntity<String> response = rest.getForEntity(url("/login"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Аптека"), "страницата трябва да е на кирилица");
        assertTrue(response.getBody().contains("_csrf"), "формата трябва да носи CSRF токен");
    }

    @Test
    @DisplayName("Стиловете се отдават като статичен ресурс")
    void stylesheetIsServed() {
        ResponseEntity<String> response = rest.getForEntity(url("/css/app.css"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(".navbar"));
    }

    @Test
    @DisplayName("Защитена страница пренасочва анонимния потребител")
    void protectedPageRedirects() {
        // TestRestTemplate следва пренасочванията, затова накрая получаваме формата за вход.
        ResponseEntity<String> response = rest.getForEntity(url("/stock"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Вход"),
                "анонимният потребител стига до формата за вход");
        assertFalse(response.getBody().contains("Наличности"),
                "съдържанието на склада не се показва без вход");
    }

    @Test
    @DisplayName("Схемата е създадена автоматично при стартиране")
    void schemaWasCreatedOnStartup() {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = SCHEMA() AND table_name IN "
                + "('users','suppliers','medicines','deliveries','batches',"
                + " 'sales','sale_items','stock_movements')",
                Integer.class);
        assertEquals(8, tables, "всичките 8 таблици трябва да съществуват");
    }
}
