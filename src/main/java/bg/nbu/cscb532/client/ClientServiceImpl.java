package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Implementation of the Client Service.
 * Handles the business logic for registering new customers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClientViewDto register(ClientRegistrationDto dto) {
        log.debug("Attempting to register new client with username: {}", dto.username());

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        String normalizedUsername = dto.username().trim();
        String normalizedEmail = dto.email().trim().toLowerCase();

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            log.warn("Registration failed. Username [{}] is already taken.", normalizedUsername);
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Registration failed. Email [{}] is already taken.", normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        String hashedPassword = passwordEncoder.encode(dto.password());

        Client newClient = new Client();
        newClient.setUsername(normalizedUsername);
        newClient.setEmail(normalizedEmail);
        newClient.setPassword(hashedPassword);
        newClient.setFirstName(dto.firstName().trim());
        newClient.setLastName(dto.lastName().trim());
        newClient.setPhoneNumber(dto.phoneNumber().trim());
        newClient.setApplicationRole(ApplicationRole.CLIENT);
        newClient.setActive(true);

        Client savedClient = clientRepository.save(newClient);
        
        log.info("Successfully registered new client with ID: {}", savedClient.getId());

        return mapToViewDto(savedClient);
    }

    private ClientViewDto mapToViewDto(Client client) {
        if (client == null) {
            return null;
        }
        return ClientViewDto.builder()
                .id(client.getId())
                .username(client.getUsername())
                .email(client.getEmail())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .phoneNumber(client.getPhoneNumber())
                .build();
    }
}
