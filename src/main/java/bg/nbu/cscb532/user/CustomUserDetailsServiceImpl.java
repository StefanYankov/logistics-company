package bg.nbu.cscb532.user;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Implementation of the custom user details service.
 * <p>
 * This service bridges our application's `UserRepository` with Spring Security's
 * `UserDetails` mechanism, facilitating the loading of users during authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user from the database by their unique username.
     * <p>
     * This method is called by Spring Security during the authentication process.
     * If the user is found, their domain properties are mapped into a Spring
     * Security `UserDetails` object, including their assigned role and active status.
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated custom user record containing the UUID (never {@code null})
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority
     */
    @Override
    public @Nonnull UserDetails loadUserByUsername(@Nonnull String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user by username: {}", username);

        User userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed. User with username [{}] not found.", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userEntity.getApplicationRole().name());

        return new CustomUserDetails(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                userEntity.getApplicationRole(),
                userEntity.isActive(),
                Collections.singletonList(authority)
        );
    }
}
