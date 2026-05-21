package bg.nbu.cscb532.company;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.base.BaseEntity;
import bg.nbu.cscb532.shared.location.AddressDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Represents the core logistic company operating the network.
 *
 * TODO: (Multi-tenancy) For a multi-tenant SaaS application, this entity would represent a tenant.
 *  Implementing multi-tenancy would involve:
 *  1. Adding a 'companyId' (or 'tenantId') foreign key to almost every other entity (User, Office, Shipment, etc.).
 *  2. Modifying the JWT structure to include a 'companyId' claim upon successful authentication.
 *  3. Implementing a custom Spring Security filter or AOP aspect to automatically inject a 'companyId'
 *     into all database queries (e.g., via Hibernate filters or a custom TenantContext).
 *  4. Ensuring all repository methods implicitly filter by the current user's 'companyId'
 *     to guarantee data isolation between tenants.
 *  5. Adjusting 'User' creation/registration to link to a specific 'Company'.
 *  This would allow a single application instance to serve multiple distinct companies, each with its own isolated data.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false, length = Constants.Validation.MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, unique = true, length = Constants.Validation.MAX_REGISTRATION_NUMBER_LENGTH)
    private String registrationNumber;

    @Embedded
    @NotNull(message = "{validation.office.address.notnull}")
    private AddressDetails addressDetails;
}
