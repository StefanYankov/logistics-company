package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.user.dto.ForgotPasswordRequestDto;
import bg.nbu.cscb532.user.dto.ResetPasswordRequestDto;
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
     * Validates a time-bound, secure token to verify a client's email address.
     * If the token is valid, the corresponding user is activated and the token is destroyed.
     *
     * @param rawToken The plain text UUID token provided to the user via email.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the token is invalid, expired, or of the wrong type.
     */
    void verifyEmail(String rawToken);

    /**
     * Retrieves a paginated list of all clients registered in the system.
     * Fulfills Requirement 5.b (All clients of the company).
     *
     * @param pageable The pagination and sorting parameters requested by the client.
     * @return A Page of view DTOs representing the clients.
     */
    Page<ClientViewDto> getAllClients(Pageable pageable);

    /**
     * Searches for clients based on a partial match (case-insensitive) of their first name,
     * last name, or phone number. Designed for autocomplete features in the UI.
     *
     * @param term     The search string provided by the user.
     * @param pageable Pagination and sorting constraints.
     * @return A Page of matching Client view DTOs.
     */
    Page<ClientViewDto> searchClients(String term, Pageable pageable);

    /**
     * Initiates the password recovery flow for a Client.
     * Generates a secure, time-bound token and dispatches an email link.
     * Silently ignores requests for non-existent emails or non-client accounts to prevent enumeration.
     *
     * @param request The DTO containing the client's email address.
     */
    void requestPasswordReset(ForgotPasswordRequestDto request);

    /**
     * Completes the password recovery flow for a Client.
     * Validates the provided secure token, updates the client's password, and revokes the token.
     *
     * @param request The DTO containing the raw token and the new password.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the token is invalid, expired, or of the wrong type.
     */
    void resetPassword(ResetPasswordRequestDto request);
}
