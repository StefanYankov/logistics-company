package bg.nbu.cscb532.user;

import bg.nbu.cscb532.shared.base.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Root entity for the application's user hierarchy, acting as the principal for Spring Security.
 * Implements a JOINED inheritance strategy, meaning shared authentication data is stored here,
 * while specific role data (like salary for employees) is stored in sub-tables.
 *
 * TODO (Tech Debt): Re-evaluate JOINED inheritance strategy.
 * Current issue: To allow walk-in Clients to register without an email, we had to make the `email`
 * column nullable at the root User level. This weakens data integrity for Employees, who SHOULD
 * always have an email.
 * FMO Solution: Refactor to "Composition over Inheritance". Create a base `UserCredentials` entity
 * mapped 1:1 with isolated `ClientProfile` and `EmployeeProfile` entities to allow strictly typed column
 * constraints for each domain.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseUUIDEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
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

    /**
     * Indicates whether the user has verified their email address.
     * Typically used to restrict access to certain features until verification is complete.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;
}
