package bg.pharmacy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Сигурност и нива на достъп.
 *
 * - Паролите се проверяват срещу BCrypt хеш (никога не се пазят в явен вид).
 * - CSRF защитата е ВКЛЮЧЕНА (по подразбиране в Spring Security); Thymeleaf
 *   автоматично добавя скрит токен във всяка форма с th:action.
 * - XSS: Thymeleaf екранира всичко изведено с th:text.
 * - Справките за продажби и печалби са само за управителя.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Справки за оборот и печалба - само управител:
                .requestMatchers("/reports/**").hasRole("MANAGER")
                // Каталог и доставчици се поддържат от управителя:
                .requestMatchers("/medicines/new", "/medicines/*/edit",
                                 "/medicines/save", "/medicines/*/active",
                                 "/suppliers/new", "/suppliers/*/edit",
                                 "/suppliers/save").hasRole("MANAGER")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(out -> out
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .sessionManagement(session -> session
                .sessionFixation(sf -> sf.migrateSession())
                .maximumSessions(1));
        return http.build();
    }
}
