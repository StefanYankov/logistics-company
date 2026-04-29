package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.User;
import bg.nbu.cscb532.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService Unit Tests")
class ClientServiceUnitTests {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Captor
    private ArgumentCaptor<Client> clientCaptor;

    // --- TEST DATA FACTORY ---
    private ClientRegistrationDto createValidRegistrationDto() {
        return ClientRegistrationDto.builder()
                .username("newCLient")
                .email("client@example.com")
                .password("rawPassword123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("0888123456")
                .build();
    }

    private Client createMockSavedClient() {
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
        return client;
    }

    @Nested
    @DisplayName("register(ClientRegistrationDto) Tests")
    class RegisterTests {

        @Test
        @DisplayName("Happy Path: Should successfully register a new client and hash password")
        void shouldRegisterSuccessfully() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            Client savedClient = createMockSavedClient();

            given(userRepository.findByUsername(dto.username())).willReturn(Optional.empty());
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.empty());
            given(passwordEncoder.encode(dto.password())).willReturn("hashedPassword123");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            ClientViewDto result = clientService.register(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(savedClient.getId());
            assertThat(result.username()).isEqualTo(dto.username());
            assertThat(result.email()).isEqualTo(dto.email());
            assertThat(result.phoneNumber()).isEqualTo(dto.phoneNumber());

            verify(userRepository).findByUsername(dto.username());
            verify(userRepository).findByEmail(dto.email());
            verify(passwordEncoder).encode(dto.password());
            
            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            
            assertThat(capturedClient.getPassword()).isEqualTo("hashedPassword123");
            assertThat(capturedClient.getApplicationRole()).isEqualTo(ApplicationRole.CLIENT);
            assertThat(capturedClient.isActive()).isTrue();
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            User existingUser = new Client();
            given(userRepository.findByUsername(dto.username())).willReturn(Optional.of(existingUser));

            // Act & Assert
            assertThatThrownBy(() -> clientService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USERNAME_DUPLICATE);

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(clientRepository);
            verify(userRepository).findByUsername(dto.username());
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            given(userRepository.findByUsername(dto.username())).willReturn(Optional.empty());

            User existingUser = new Client();
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.of(existingUser));

            // Act & Assert
            assertThatThrownBy(() -> clientService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(clientRepository);
        }
        
        @Test
        @DisplayName("Edge Case: Should normalize inputs before checking duplicates and saving")
        void shouldNormalizeInputsBeforeProcessing() {

            // Arrange
            ClientRegistrationDto dirtyDto = ClientRegistrationDto.builder()
                .username("  spacedUser  ")
                .email("  UPPER@Example.com  ")
                .password("rawPassword123")
                .firstName(" John ")
                .lastName(" Doe ")
                .phoneNumber(" 0888123456 ")
                .build();
                
            Client savedClient = createMockSavedClient();
            
            given(userRepository.findByUsername("spacedUser")).willReturn(Optional.empty());
            given(userRepository.findByEmail("upper@example.com")).willReturn(Optional.empty());
            given(passwordEncoder.encode(anyString())).willReturn("hash");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            clientService.register(dirtyDto);

            // Assert
            verify(userRepository).findByUsername("spacedUser");
            verify(userRepository).findByEmail("upper@example.com");
            
            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            
            assertThat(capturedClient.getUsername()).isEqualTo("spacedUser");
            assertThat(capturedClient.getEmail()).isEqualTo("upper@example.com");
            assertThat(capturedClient.getFirstName()).isEqualTo("John");
            assertThat(capturedClient.getLastName()).isEqualTo("Doe");
            assertThat(capturedClient.getPhoneNumber()).isEqualTo("0888123456");
        }
    }

    @Nested
    @DisplayName("getAllClients(Pageable) Tests")
    class GetAllClientsTests {

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list of all clients")
        void shouldGetAllClientsSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Client mockClient = createMockSavedClient();
            Page<Client> pagedResponse = new PageImpl<>(List.of(mockClient), pageable, 1);

            given(clientRepository.findAll(pageable)).willReturn(pagedResponse);

            // Act
            Page<ClientViewDto> result = clientService.getAllClients(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            
            ClientViewDto dto = result.getContent().getFirst();
            assertThat(dto.id()).isEqualTo(mockClient.getId());
            assertThat(dto.username()).isEqualTo("newCLient");
            assertThat(dto.isActive()).isTrue();

            verify(clientRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Edge Case: Should return empty page if no clients exist")
        void shouldReturnEmptyPageWhenNoClients() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Client> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(clientRepository.findAll(pageable)).willReturn(emptyPage);

            // Act
            Page<ClientViewDto> result = clientService.getAllClients(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();

            verify(clientRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Error Case: Defense in depth - should throw NullPointerException if Pageable is null")
        void shouldFailFastIfPageableNull() {
            assertThatThrownBy(() -> clientService.getAllClients(null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(clientRepository);
        }
    }
}