package bg.pharmacy.service;

import bg.pharmacy.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Spring Security взема потребителите от таблица users (пароли като BCrypt хешове). */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsername(username)
                    .map(AppUserPrincipal::new)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Няма потребител с име: " + username));
    }
}
