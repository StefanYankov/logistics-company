package bg.nbu.cscb532.client.service;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.client.ClientServiceImpl;
import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.shared.infrastructure.email.EmailService;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.UserRepository;
import bg.nbu.cscb532.user.VerificationToken;
import bg.nbu.cscb532.user.VerificationTokenRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

public abstract class AbstractClientUnitTestBase {

    @Mock protected ClientRepository clientRepository;
    @Mock protected UserRepository userRepository;
    @Mock protected PasswordEncoder passwordEncoder;
    @Mock protected VerificationTokenRepository verificationTokenRepository;
    @Mock protected EmailService emailService;

    @InjectMocks protected ClientServiceImpl clientService;

    @Captor protected ArgumentCaptor<Client> clientCaptor;
    @Captor protected ArgumentCaptor<VerificationToken> tokenCaptor;

    // --- TEST DATA FACTORY ---
    protected ClientRegistrationDto createValidRegistrationDto() {
        return ClientRegistrationDto.builder()
                .username("newCLient")
                .email("client@example.com")
                .password("rawPassword123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("0888123456")
                .build();
    }

    protected Client createMockSavedClient() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setUsername("newCLient");
        client.setEmail("client@example.com");
        client.setPassword("hashedPassword123");
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setPhoneNumber("0888123456");
        client.setApplicationRole(ApplicationRole.CLIENT);
        client.setActive(true);
        client.setEmailVerified(false);
        return client;
    }
}
