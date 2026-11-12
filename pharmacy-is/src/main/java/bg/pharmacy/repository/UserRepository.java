package bg.pharmacy.repository;

import bg.pharmacy.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Параметърът се подава подготвено (?), а не чрез конкатенация - защита от SQL Injection. */
    public Optional<User> findByUsername(String username) {
        List<User> found = jdbc.query(
                "SELECT id, username, password_hash, full_name, role, active "
                + "FROM users WHERE username = ?",
                RowMappers.USER, username);
        return found.stream().findFirst();
    }
}
