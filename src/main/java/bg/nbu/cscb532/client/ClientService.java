package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the Client aggregate root.
 */
public interface ClientService {

    /**
     * Registers a new client in the system.
     *
     * @param dto The DTO containing the client's registration details.
     * @return A view DTO of the newly created client.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the username or email is already taken.
     */
    ClientViewDto register(ClientRegistrationDto dto);

    /**
     * Retrieves a paginated list of all clients registered in the system.
     * Fulfills Requirement 5.b (All clients of the company).
     *
     * @param pageable The pagination and sorting parameters requested by the client.
     * @return A Page of view DTOs representing the clients.
     */
    Page<ClientViewDto> getAllClients(Pageable pageable);
}
