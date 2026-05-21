package bg.nbu.cscb532.client.service;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.user.TokenType;
import bg.nbu.cscb532.user.VerificationToken;
import bg.nbu.cscb532.user.dto.ForgotPasswordRequestDto;
import bg.nbu.cscb532.user.dto.ResetPasswordRequestDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService: Account Security Tests")
public class ClientAccountSecurityTests extends AbstractClientUnitTestBase {

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
            Assertions.assertThat(clientCaptor.getValue().isEmailVerified()).isTrue();

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
            Assertions.assertThat(clientCaptor.getValue().getPassword()).isEqualTo("new-hashed-password");

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
    @DisplayName("deactivate(UUID) Tests")
    class DeactivateTests {

        @Test
        @DisplayName("Happy Path: Should successfully soft delete a client")
        void shouldDeactivateClient() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            Client client = new Client();
            client.setId(clientId);
            client.setActive(true);

            given(clientRepository.findById(clientId)).willReturn(Optional.of(client));

            // Act
            clientService.deactivate(clientId);

            // Assert
            verify(clientRepository).save(clientCaptor.capture());
            Assertions.assertThat(clientCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when client not found")
        void shouldThrowExceptionWhenClientNotFound() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            given(clientRepository.findById(clientId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> clientService.deactivate(clientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("activate(UUID) Tests")
    class ActivateTests {

        @Test
        @DisplayName("Happy Path: Should successfully activate a deactivated client")
        void shouldActivateClient() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            Client client = new Client();
            client.setId(clientId);
            client.setActive(false);

            given(clientRepository.findById(clientId)).willReturn(Optional.of(client));

            // Act
            clientService.activate(clientId);

            // Assert
            verify(clientRepository).save(clientCaptor.capture());
            Assertions.assertThat(clientCaptor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("Error Case: Should throw BusinessException when client not found")
        void shouldThrowExceptionWhenClientNotFound() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            given(clientRepository.findById(clientId)).willReturn(Optional.empty());

            // Act and Assert
            assertThatThrownBy(() -> clientService.activate(clientId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verify(clientRepository, never()).save(any());
        }
    }
}
