package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientQuickRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientUpdateDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.infrastructure.email.EmailService;
import bg.nbu.cscb532.shared.security.SecurityUtils;
import bg.nbu.cscb532.user.*;
import bg.nbu.cscb532.user.dto.ForgotPasswordRequestDto;
import bg.nbu.cscb532.user.dto.ResetPasswordRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

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
        String normalizedPhone = dto.phoneNumber().trim();

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            log.warn("Registration failed. Username [{}] is already taken.", normalizedUsername);
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Registration failed. Email [{}] is already taken.", normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        String hashedPassword = passwordEncoder.encode(dto.password());

        // Claim Account Logic
        // TODO: (Tech Debt) Account claiming currently relies solely on matching a phone number.
        // This is vulnerable to identity spoofing (IDOR). To safely allow a user to claim historical
        // "walk-in" or "guest receiver" shipments, we MUST implement an SMS OTP (One-Time Password)
        // verification flow to cryptographically prove phone number ownership before merging accounts.
        Optional<Client> existingClientOpt = clientRepository.findByPhoneNumber(normalizedPhone);
        
        Client targetClient;

        if (existingClientOpt.isPresent()) {
            Client existing = existingClientOpt.get();
            // A pure offline account will have a null email and a generated username starting with "walkin_"
            if (existing.getEmail() == null && existing.getUsername().startsWith("walkin_")) {
                log.info("Found existing offline account for phone [{}]. Merging (claiming) account.", normalizedPhone);
                targetClient = existing;
                // Update the existing offline profile with the new digital credentials
                targetClient.setUsername(normalizedUsername);
                targetClient.setEmail(normalizedEmail);
                targetClient.setPassword(hashedPassword);
                targetClient.setFirstName(dto.firstName().trim());
                targetClient.setLastName(dto.lastName().trim());
                // Phone number is already set
                targetClient.setEmailVerified(false);
            } else {
                // The phone number belongs to someone who already has a digital account or an email
                log.warn("Registration failed. Phone number [{}] is already fully registered to another account.", normalizedPhone);
                throw new BusinessException(ErrorCode.PHONE_DUPLICATE);
            }
        } else {
            // Standard flow: create a brand new client
            targetClient = new Client();
            targetClient.setUsername(normalizedUsername);
            targetClient.setEmail(normalizedEmail);
            targetClient.setPassword(hashedPassword);
            targetClient.setFirstName(dto.firstName().trim());
            targetClient.setLastName(dto.lastName().trim());
            targetClient.setPhoneNumber(normalizedPhone);
            targetClient.setApplicationRole(ApplicationRole.CLIENT);
            targetClient.setActive(true);
            targetClient.setEmailVerified(false);
        }

        Client savedClient = clientRepository.save(targetClient);

        // Security best practice: If the user clicked resend, we invalidate older tokens
        verificationTokenRepository.findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(
                savedClient.getId(), TokenType.EMAIL_VERIFICATION
        ).ifPresent(verificationTokenRepository::delete);

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = SecurityUtils.hashSha256(rawToken);

        VerificationToken verificationToken = VerificationToken.builder()
                .tokenHash(hashedToken)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiryDate(Instant.now().plus(Duration.ofHours(24)))
                .user(savedClient)
                .build();

        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedClient.getEmail(), rawToken);

        log.info("Client registered/claimed in pending state. Verification email sent to: {}", savedClient.getEmail());

        return mapToViewDto(savedClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClientViewDto quickRegister(ClientQuickRegistrationDto dto) {
        log.debug("Attempting quick registration for walk-in client: {} {}", dto.firstName(), dto.lastName());

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);
        
        String normalizedPhone = dto.phoneNumber().trim();
        String normalizedEmail = dto.email() != null && !dto.email().isBlank() ? dto.email().trim().toLowerCase() : null;

        // Check if phone number already exists
        if (clientRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
             log.warn("Quick registration failed. Phone number [{}] is already registered.", normalizedPhone);
             throw new BusinessException(ErrorCode.PHONE_DUPLICATE);
        }

        if (normalizedEmail != null && userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Quick registration failed. Email [{}] is already registered.", normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        // Generate system defaults for required fields
        String generatedUsername = "walkin_" + UUID.randomUUID().toString().substring(0, 8);
        // Generate a random password
        String generatedPassword = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(generatedPassword);

        Client newClient = new Client();
        newClient.setUsername(generatedUsername);
        newClient.setEmail(normalizedEmail); // Might be null
        newClient.setPassword(hashedPassword);
        newClient.setFirstName(dto.firstName().trim());
        newClient.setLastName(dto.lastName().trim());
        newClient.setPhoneNumber(normalizedPhone);
        newClient.setApplicationRole(ApplicationRole.CLIENT);
        newClient.setActive(true);
        newClient.setEmailVerified(false);

        Client savedClient = clientRepository.save(newClient);

        log.info("Walk-in client successfully registered with system ID: {}", savedClient.getId());

        // If an email was provided during quick registration, initiate the password reset flow
        // so they can claim their account online.
        if (normalizedEmail != null) {
            log.debug("Email provided during quick registration. Initiating password reset flow to allow account claim.");
            ForgotPasswordRequestDto resetRequest = new ForgotPasswordRequestDto();
            resetRequest.setEmail(normalizedEmail);
            this.requestPasswordReset(resetRequest);
        }

        return mapToViewDto(savedClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ClientViewDto updateClientProfile(UUID id, ClientUpdateDto dto) {
        log.debug("Attempting to update profile for client ID: {}", id);

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        String normalizedPhone = dto.phoneNumber().trim();

        // If they are changing their phone number, ensure it doesn't conflict with someone else
        if (!client.getPhoneNumber().equals(normalizedPhone)) {
            if (clientRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
                log.warn("Profile update failed. Phone number [{}] is already taken.", normalizedPhone);
                throw new BusinessException(ErrorCode.PHONE_DUPLICATE);
            }
        }

        client.setFirstName(dto.firstName().trim());
        client.setLastName(dto.lastName().trim());
        client.setPhoneNumber(normalizedPhone);

        Client updatedClient = clientRepository.save(client);

        log.info("Successfully updated profile for client ID: {}", id);

        return mapToViewDto(updatedClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ClientViewDto getClientById(UUID id) {
        log.debug("Fetching client by ID: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return mapToViewDto(client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClientViewDto> getAllClients(Pageable pageable) {
        log.debug("Fetching paginated list of all clients");

        Objects.requireNonNull(pageable, Constants.DeveloperErrors.PAGEABLE_NULL);

        return clientRepository.findAll(pageable)
                .map(this::mapToViewDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClientViewDto> searchClients(String term, Pageable pageable) {
        log.debug("Searching for clients with term: {}", term);

        Objects.requireNonNull(pageable, Constants.DeveloperErrors.PAGEABLE_NULL);

        if (term == null || term.isBlank()) {
            return Page.empty(pageable);
        }

        return clientRepository.searchClients(term.trim(), pageable)
                .map(this::mapToViewDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        log.debug("Attempting to verify email via token");

        if (rawToken == null || rawToken.isBlank()) {
            log.warn("Email verification failed: Empty token provided");
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        String hashedToken = SecurityUtils.hashSha256(rawToken);

        VerificationToken verificationToken = verificationTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> {
                    log.warn("Email verification failed: Token not found in database (invalid or already used)");
                    return new BusinessException(ErrorCode.INVALID_TOKEN);
                });

        if (verificationToken.getTokenType() != TokenType.EMAIL_VERIFICATION) {
            log.warn("Email verification failed: Provided token is of wrong type [{}]", verificationToken.getTokenType());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (!verificationToken.isValid()) {
            log.warn("Email verification failed: Token has expired for user [{}]", verificationToken.getUser().getUsername());
            verificationTokenRepository.delete(verificationToken); // Clean up the expired token
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken); // Consume the token (Single-use security)

        log.info("Successfully verified email for user [{}]", user.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequestDto request) {
        Objects.requireNonNull(request, Constants.DeveloperErrors.DTO_NULL);
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        
        log.debug("Attempting to initiate password reset for email: {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        // Security: Prevent User Enumeration. Do not throw an error if the email doesn't exist.
        // We also explicitly restrict self-service resets to CLIENT roles only.
        if (user == null || user.getApplicationRole() != ApplicationRole.CLIENT) {
            log.warn("Password reset ignored: Email [{}] not found or user is not a Client.", normalizedEmail);
            return; 
        }

        // Invalidate any previously requested, unused reset tokens to prevent token hoarding
        verificationTokenRepository.findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(
                user.getId(), TokenType.PASSWORD_RESET
        ).ifPresent(verificationTokenRepository::delete);

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = SecurityUtils.hashSha256(rawToken);

        VerificationToken resetToken = VerificationToken.builder()
                .tokenHash(hashedToken)
                .tokenType(TokenType.PASSWORD_RESET)
                .expiryDate(Instant.now().plus(Duration.ofHours(1))) // Reset tokens expire much faster than verification
                .user(user)
                .build();

        verificationTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);

        log.info("Password reset token generated and sent to: {}", user.getEmail());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        log.debug("Attempting to execute password reset via token.");

        Objects.requireNonNull(request, Constants.DeveloperErrors.DTO_NULL);
        
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        String hashedToken = SecurityUtils.hashSha256(request.getToken());

        VerificationToken tokenEntity = verificationTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> {
                    log.warn("Password reset failed: Token not found in database.");
                    return new BusinessException(ErrorCode.INVALID_TOKEN);
                });

        if (tokenEntity.getTokenType() != TokenType.PASSWORD_RESET) {
            log.warn("Password reset failed: Provided token is of wrong type [{}]", tokenEntity.getTokenType());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (!tokenEntity.isValid()) {
            log.warn("Password reset failed: Token has expired for user [{}]", tokenEntity.getUser().getUsername());
            verificationTokenRepository.delete(tokenEntity);
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = tokenEntity.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepository.delete(tokenEntity); // Burn the token

        log.info("Successfully reset password for user [{}]", user.getUsername());
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
                .isActive(client.isActive())
                .isEmailVerified(client.isEmailVerified())
                .build();
    }
}
