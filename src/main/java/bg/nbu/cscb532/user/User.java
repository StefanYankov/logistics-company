package bg.nbu.cscb532.user;

import bg.nbu.cscb532.shared.base.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Root entity for the application's user hierarchy, acting as the principal for Spring Security.
 * Implements a JOINED inheritance strategy, meaning shared authentication data is stored here,
 * while specific role data (like salary for employees) is stored in sub-tables.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseUUIDEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name= "application_role",nullable = false)
    private ApplicationRole applicationRole;

    /**
     * Soft-delete flag used by Spring Security.
     * If false, the user cannot authenticate and their JWTs will be rejected.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}