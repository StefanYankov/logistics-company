package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientQuickRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.infrastructure.email.EmailService;
import bg.nbu.cscb532.user.*;
import bg.nbu.cscb532.user.dto.ForgotPasswordRequestDto;
import bg.nbu.cscb532.user.dto.ResetPasswordRequestDto;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService Unit Tests")
class ClientServiceUnitTests {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Captor
    private ArgumentCaptor<Client> clientCaptor;

    @Captor
    private ArgumentCaptor<VerificationToken> tokenCaptor;

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
        client.setEmailVerified(false);
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

            // Assert: API Result
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(savedClient.getId());
            assertThat(result.username()).isEqualTo(dto.username());

            // Assert: Client State Verification
            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            assertThat(capturedClient.getPassword()).isEqualTo("hashedPassword123");
            assertThat(capturedClient.getApplicationRole()).isEqualTo(ApplicationRole.CLIENT);
            assertThat(capturedClient.isActive()).isTrue();
            assertThat(capturedClient.isEmailVerified()).isFalse();

            // Assert: Security Token Verification
            verify(verificationTokenRepository).save(tokenCaptor.capture());
            VerificationToken capturedToken = tokenCaptor.getValue();
            assertThat(capturedToken.getTokenType()).isEqualTo(TokenType.EMAIL_VERIFICATION);
            assertThat(capturedToken.getUser()).isEqualTo(savedClient);
            assertThat(capturedToken.getTokenHash()).hasSize(64); // SHA-256 Hex length
            assertThat(capturedToken.getExpiryDate()).isAfter(Instant.now());

            // Assert: Infrastructure Side Effects
            verify(emailService).sendVerificationEmail(eq(dto.email()), anyString());

            verify(userRepository).findByUsername(dto.username());
            verify(userRepository).findByEmail(dto.email());
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            User existingUser = new Client();
            given(userRepository.findByUsername(dto.username())).willReturn(Optional.of(existingUser));

            // Act and Assert
            assertThatThrownBy(() -> clientService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USERNAME_DUPLICATE);

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(clientRepository);
            verifyNoInteractions(verificationTokenRepository);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {

            // Arrange
            ClientRegistrationDto dto = createValidRegistrationDto();
            given(userRepository.findByUsername(dto.username())).willReturn(Optional.empty());

            User existingUser = new Client();
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.of(existingUser));

            // Act and Assert
            assertThatThrownBy(() -> clientService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(clientRepository);
            verifyNoInteractions(verificationTokenRepository);
            verifyNoInteractions(emailService);
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

            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());
            savedClient.setEmail("upper@example.com");
            savedClient.setUsername("spacedUser");

            given(userRepository.findByUsername("spacedUser")).willReturn(Optional.empty());
            given(userRepository.findByEmail("upper@example.com")).willReturn(Optional.empty());
            given(passwordEncoder.encode(anyString())).willReturn("hash");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            clientService.register(dirtyDto);

            // Assert
            verify(userRepository).findByUsername("spacedUser");
            verify(userRepository).findByEmail("upper@example.com");
            verify(emailService).sendVerificationEmail(eq("upper@example.com"), anyString());

            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            assertThat(capturedClient.getUsername()).isEqualTo("spacedUser");
            assertThat(capturedClient.getEmail()).isEqualTo("upper@example.com");
            assertThat(capturedClient.getFirstName()).isEqualTo("John");
        }
    }
    
    @Nested
    @DisplayName("quickRegister(ClientQuickRegistrationDto) Tests")
    class QuickRegisterTests {

        @Test
        @DisplayName("Happy Path: Should successfully create client without email and auto-generate credentials")
        void shouldQuickRegisterSuccessfullyWithoutEmail() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Grandma")
                    .lastName("Smith")
                    .phoneNumber("0888999111")
                    .build();
                    
            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());
            savedClient.setPhoneNumber(dto.phoneNumber());
            
            Page<Client> emptyPage = new PageImpl<>(List.of());
            given(clientRepository.searchClients(dto.phoneNumber(), Pageable.unpaged())).willReturn(emptyPage);
            
            given(passwordEncoder.encode(anyString())).willReturn("hashed-auto-password");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            ClientViewDto result = clientService.quickRegister(dto);

            // Assert
            assertThat(result).isNotNull();
            
            verify(clientRepository).save(clientCaptor.capture());
            Client capturedClient = clientCaptor.getValue();
            
            assertThat(capturedClient.getFirstName()).isEqualTo("Grandma");
            assertThat(capturedClient.getPhoneNumber()).isEqualTo("0888999111");
            assertThat(capturedClient.getEmail()).isNull();
            assertThat(capturedClient.getUsername()).startsWith("walkin_");
            assertThat(capturedClient.getPassword()).isEqualTo("hashed-auto-password");
            assertThat(capturedClient.getApplicationRole()).isEqualTo(ApplicationRole.CLIENT);
            
            verifyNoInteractions(verificationTokenRepository, emailService);
        }

        @Test
        @DisplayName("Happy Path: Should create client and trigger password reset if email is provided")
        void shouldQuickRegisterAndTriggerResetWithEmail() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Busy")
                    .lastName("Mom")
                    .phoneNumber("0888555444")
                    .email("mom@example.com")
                    .build();
                    
            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());
            savedClient.setEmail(dto.email());
            savedClient.setApplicationRole(ApplicationRole.CLIENT);
            
            Page<Client> emptyPage = new PageImpl<>(List.of());
            given(clientRepository.searchClients(dto.phoneNumber(), Pageable.unpaged())).willReturn(emptyPage);
            
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.empty());
            given(passwordEncoder.encode(anyString())).willReturn("hash");
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);
            reset(userRepository);
            when(userRepository.findByEmail(dto.email()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedClient));

            // Act
            clientService.quickRegister(dto);

            // Assert
            verify(clientRepository).save(clientCaptor.capture());
            assertThat(clientCaptor.getValue().getEmail()).isEqualTo("mom@example.com");
            
            verify(verificationTokenRepository).save(any(VerificationToken.class));
            verify(emailService).sendPasswordResetEmail(eq("mom@example.com"), anyString());
        }

        @Test
        @DisplayName("Error Case: Should throw PHONE_DUPLICATE if phone number exists")
        void shouldThrowIfPhoneDuplicate() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("0888111222")
                    .build();
                    
            Page<Client> existingPage = new PageImpl<>(List.of(new Client()));
            given(clientRepository.searchClients(dto.phoneNumber(), Pageable.unpaged())).willReturn(existingPage);

            // Act and Assert
            assertThatThrownBy(() -> clientService.quickRegister(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PHONE_DUPLICATE);
                    
            verifyNoInteractions(passwordEncoder, userRepository);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should throw EMAIL_DUPLICATE if provided email exists")
        void shouldThrowIfEmailDuplicate() {
            // Arrange
            ClientQuickRegistrationDto dto = ClientQuickRegistrationDto.builder()
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("0888111222")
                    .email("existing@test.com")
                    .build();
                    
            Page<Client> emptyPage = new PageImpl<>(List.of());
            given(clientRepository.searchClients(dto.phoneNumber(), Pageable.unpaged())).willReturn(emptyPage);
            given(userRepository.findByEmail(dto.email())).willReturn(Optional.of(new Client()));

            // Act and Assert
            assertThatThrownBy(() -> clientService.quickRegister(dto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);
                    
            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyEmail(String rawToken) Tests")
    class VerifyEmailTests {

        @Test
        @DisplayName("Happy Path: Should successfully verify email, activate user, and consume token")
        void shouldVerifyEmailSuccessfully() {
            // Arrange
            String rawToken = UUID.randomUUID().toString();
            Client unverifiedClient = createMockSavedClient();
            unverifiedClient.setEmailVerified(false);

            VerificationToken validToken = VerificationToken.builder()
                    .tokenHash("some-hash")
                    .tokenType(TokenType.EMAIL_VERIFICATION)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                    .user(unverifiedClient)
                    .build();

            given(verificationTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(validToken));

            // Act
            clientService.verifyEmail(rawToken);

            // Assert
            verify(userRepository).save(clientCaptor.capture());
            assertThat(clientCaptor.getValue().isEmailVerified()).isTrue();

            verify(verificationTokenRepository).delete(validToken);
        }

        @Test
        @DisplayName("Validation Error: Should throw BusinessException when token is null or blank")
        void shouldThrowWhenTokenIsBlank() {
            assertThatThrownBy(() -> clientService.verifyEmail("   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verifyNoInteractions(verificationTokenRepository, userRepository);
        }

        @Test
        @DisplayName("Security Error: Should throw INVALID_TOKEN when token hash not found in database")
        void shouldThrowWhenTokenNotFound() {
            String rawToken = UUID.randomUUID().toString();
            given(verificationTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.verifyEmail(rawToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Security Error: Should throw EXPIRED_TOKEN and clean up DB when token is expired")
        void shouldThrowWhenTokenExpired() {
            String rawToken = UUID.randomUUID().toString();
            Client unverifiedClient = createMockSavedClient();

            VerificationToken expiredToken = VerificationToken.builder()
                    .tokenHash("some-hash")
                    .tokenType(TokenType.EMAIL_VERIFICATION)
                    .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                    .user(unverifiedClient)
                    .build();

            given(verificationTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> clientService.verifyEmail(rawToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXPIRED_TOKEN);

            verify(verificationTokenRepository).delete(expiredToken);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Security Error: Should throw INVALID_TOKEN when provided a PASSWORD_RESET token instead of verification")
        void shouldThrowWhenWrongTokenType() {
            String rawToken = UUID.randomUUID().toString();
            Client unverifiedClient = createMockSavedClient();

            VerificationToken wrongTypeToken = VerificationToken.builder()
                    .tokenHash("some-hash")
                    .tokenType(TokenType.PASSWORD_RESET)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                    .user(unverifiedClient)
                    .build();

            given(verificationTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(wrongTypeToken));

            assertThatThrownBy(() -> clientService.verifyEmail(rawToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);

            verify(verificationTokenRepository, never()).delete(any());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("Password Reset Flow Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("requestPasswordReset: Happy Path - Should generate token and send email for a valid client")
        void shouldRequestPasswordResetSuccessfully() {
            // Arrange
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("client@example.com");
            Client existingClient = createMockSavedClient();
            
            given(userRepository.findByEmail("client@example.com")).willReturn(Optional.of(existingClient));

            // Act
            clientService.requestPasswordReset(request);

            // Assert
            verify(verificationTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getTokenType()).isEqualTo(TokenType.PASSWORD_RESET);
            assertThat(tokenCaptor.getValue().getUser()).isEqualTo(existingClient);

            verify(emailService).sendPasswordResetEmail(eq("client@example.com"), anyString());
        }

        @Test
        @DisplayName("requestPasswordReset: Security - Should silently ignore non-existent emails")
        void shouldSilentlyIgnoreNonExistentEmail() {
            // Arrange
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("unknown@example.com");
            given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

            // Act
            clientService.requestPasswordReset(request);

            // Assert
            verifyNoInteractions(verificationTokenRepository, emailService);
        }

        @Test
        @DisplayName("resetPassword: Happy Path - Should update password and consume token")
        void shouldResetPasswordSuccessfully() {
            // Arrange
            ResetPasswordRequestDto request = new ResetPasswordRequestDto(UUID.randomUUID().toString(), "newStrongPassword!");
            Client client = createMockSavedClient();
            
            VerificationToken validToken = VerificationToken.builder()
                    .tokenHash("some-hash")
                    .tokenType(TokenType.PASSWORD_RESET)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                    .user(client)
                    .build();
            
            given(verificationTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(validToken));
            given(passwordEncoder.encode("newStrongPassword!")).willReturn("new-hashed-password");

            // Act
            clientService.resetPassword(request);

            // Assert
            verify(userRepository).save(clientCaptor.capture());
            assertThat(clientCaptor.getValue().getPassword()).isEqualTo("new-hashed-password");

            verify(verificationTokenRepository).delete(validToken);
        }

        @Test
        @DisplayName("resetPassword: Security Error - Should throw INVALID_TOKEN for wrong token type")
        void shouldThrowForWrongResetTokenType() {
            // Arrange
            ResetPasswordRequestDto request = new ResetPasswordRequestDto(UUID.randomUUID().toString(), "newPass");
            Client client = createMockSavedClient();
            
            VerificationToken wrongTypeToken = VerificationToken.builder()
                    .tokenHash("some-hash")
                    .tokenType(TokenType.EMAIL_VERIFICATION)
                    .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                    .user(client)
                    .build();

            given(verificationTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(wrongTypeToken));

            // Act and Assert
            assertThatThrownBy(() -> clientService.resetPassword(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
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

            verify(clientRepository).findAll(pageable);
            verifyNoInteractions(emailService);
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
            verifyNoInteractions(emailService);

        }
    }

    @Nested
    @DisplayName("searchClients(String, Pageable) Tests")
    class SearchClientsTests {

        @Test
        @DisplayName("Happy Path: Should retrieve paginated list of matching clients")
        void shouldSearchClientsSuccessfully() {
            // Arrange
            String term = "Doe";
            Pageable pageable = PageRequest.of(0, 10);
            Client mockClient = createMockSavedClient();
            Page<Client> pagedResponse = new PageImpl<>(List.of(mockClient), pageable, 1);

            given(clientRepository.searchClients(term, pageable)).willReturn(pagedResponse);

            // Act
            Page<ClientViewDto> result = clientService.searchClients(term, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().lastName()).isEqualTo("Doe");

            verify(clientRepository).searchClients(term, pageable);
        }

        @Test
        @DisplayName("Edge Case: Should trim term and search")
        void shouldTrimTermAndSearch() {
            // Arrange
            String dirtyTerm = "  0888  ";
            String cleanTerm = "0888";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Client> pagedResponse = new PageImpl<>(List.of(createMockSavedClient()), pageable, 1);

            given(clientRepository.searchClients(cleanTerm, pageable)).willReturn(pagedResponse);

            // Act
            clientService.searchClients(dirtyTerm, pageable);

            // Assert
            verify(clientRepository).searchClients(cleanTerm, pageable);
        }

        @Test
        @DisplayName("Edge Case: Should return empty page if term is null or blank")
        void shouldReturnEmptyPageIfTermBlank() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<ClientViewDto> result = clientService.searchClients("   ", pageable);

            // Assert
            assertThat(result.isEmpty()).isTrue();
            verifyNoInteractions(clientRepository);
        }
    }
}