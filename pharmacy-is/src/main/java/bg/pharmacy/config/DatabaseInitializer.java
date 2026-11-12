package bg.pharmacy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Създава схемата при първо стартиране, ако таблиците още ги няма.
 *
 * Изпълняват се същите файлове от папка database/, които могат да се
 * импортират и ръчно - няма втора, различна версия на схемата.
 * Изключва се с pharmacy.auto-init-database=false.
 */
@Component
public class DatabaseInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final String SCHEMA_SCRIPT = "db/01_schema.sql";
    private static final String SEED_SCRIPT   = "db/02_seed.sql";
    private static final String PROBE_TABLE   = "medicines";

    private final DataSource dataSource;
    private final boolean enabled;
    private final boolean seed;
    private final String jdbcUrl;
    private final String jdbcUser;

    public DatabaseInitializer(DataSource dataSource,
                               @Value("${pharmacy.auto-init-database:true}") boolean enabled,
                               @Value("${pharmacy.auto-seed-demo-data:true}") boolean seed,
                               @Value("${spring.datasource.url:(не е зададен)}") String jdbcUrl,
                               @Value("${spring.datasource.username:(не е зададен)}") String jdbcUser) {
        this.dataSource = dataSource;
        this.enabled = enabled;
        this.seed = seed;
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!enabled) {
            log.info("Автоматичното създаване на схемата е изключено "
                     + "(pharmacy.auto-init-database=false).");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            if (tableExists(connection, PROBE_TABLE)) {
                log.info("Базата данни е налична - схемата не се променя.");
                return;
            }
            log.info("Таблица \"{}\" липсва - създавам схемата от {}", PROBE_TABLE, SCHEMA_SCRIPT);
            run(connection, SCHEMA_SCRIPT);
            if (seed) {
                log.info("Зареждам примерни данни от {}", SEED_SCRIPT);
                run(connection, SEED_SCRIPT);
            }
            log.info("Базата данни е готова за работа.");
        } catch (SQLException ex) {
            // Най-честата грешка при пренасяне на друг компютър. Вместо стек от
            // изключения показваме какво точно трябва да се направи.
            throw new IllegalStateException(friendlyMessage(ex), ex);
        }
    }

    private String friendlyMessage(SQLException ex) {
        return String.join(System.lineSeparator(),
                "",
                "============================================================",
                " НЕУСПЕШНА ВРЪЗКА С БАЗАТА ДАННИ",
                "============================================================",
                " Причина: " + ex.getMessage(),
                "",
                " Проверете по ред:",
                "   1. Работи ли MySQL сървърът?",
                "        Linux:   sudo systemctl start mysqld",
                "        Windows: стартирайте MySQL от услугите или от XAMPP",
                "   2. Създадена ли е базата? Пуснете веднъж:",
                "        mysql -u root -p < database/00_create_database.sql",
                "   3. Съвпадат ли потребителят и паролата в",
                "        src/main/resources/application.properties",
                "",
                " Текуща настройка: " + jdbcUrl,
                " Потребител:       " + jdbcUser,
                "============================================================",
                "");
    }

    private void run(Connection connection, String path) {
        Resource script = new ClassPathResource(path);
        if (!script.exists()) {
            throw new IllegalStateException("Липсва SQL скрипт в classpath: " + path);
        }
        ScriptUtils.executeSqlScript(connection,
                new EncodedResource(script, StandardCharsets.UTF_8));
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        String catalog = connection.getCatalog();
        for (String name : new String[]{table, table.toUpperCase()}) {
            try (ResultSet rs = connection.getMetaData()
                    .getTables(catalog, null, name, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
        }
        return false;
    }
}
