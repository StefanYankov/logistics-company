package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;

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
}
