package bg.pharmacy.config;

import bg.pharmacy.logic.ExpiryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * Колко дни преди изтичане на срока да се предупреждава.
     * Настройва се с pharmacy.expiry-warning-days в application.properties.
     */
    @Bean
    public ExpiryPolicy expiryPolicy(
            @Value("${pharmacy.expiry-warning-days:90}") int warningDays) {
        return new ExpiryPolicy(warningDays);
    }
}
