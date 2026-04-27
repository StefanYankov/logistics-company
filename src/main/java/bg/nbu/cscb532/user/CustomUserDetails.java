package bg.nbu.cscb532.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom extension of Spring Security's User object.
 * We use this to cache domain-specific fields (like the database UUID and the ApplicationRole)
 * directly in the Spring Security Context in memory.
 * This prevents the application from having to query the database by username on every single API request
 * just to figure out "who" is making the request, vastly improving performance for operations
 * like registering shipments or enforcing row-level visibility rules.
 */
@Getter
public class CustomUserDetails extends User {

    private final UUID id;
    private final ApplicationRole applicationRole;

    /**
     * Constructor mapping our database User entity to the Spring Security User object.
     */
    public CustomUserDetails(
            UUID id,
            String username,
            String password,
            ApplicationRole applicationRole,
            boolean isActive,
            Collection<? extends GrantedAuthority> authorities) {
        
        super(username, password, isActive, true, true, true, authorities);
        this.id = id;
        this.applicationRole = applicationRole;
    }
}
