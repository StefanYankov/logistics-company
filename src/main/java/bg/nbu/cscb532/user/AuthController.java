package bg.nbu.cscb532.user;

import bg.nbu.cscb532.client.ClientService;
import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import bg.nbu.cscb532.user.dto.ForgotPasswordRequestDto;
import bg.nbu.cscb532.user.dto.LoginRequestDto;
import bg.nbu.cscb532.user.dto.LoginResponseDto;
import bg.nbu.cscb532.user.dto.ResetPasswordRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling public authentication endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "Endpoints for user login and token generation.")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ClientService clientService; // Injected to handle self-service password resets

    @Operation(
            summary = "Authenticate user",
            description = "Validates username and password. Returns a JWT if successful."
    )
    @ApiResponse(responseCode = "200", description = "Successfully authenticated")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., missing fields)")
    @ApiResponse(responseCode = "401", description = "Invalid credentials or inactive account")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("API POST request to authenticate user: {}", request.username());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String jwtToken = jwtService.generateToken(userDetails);

        log.info("Successfully authenticated user: {}. Token generated.", request.username());

        return ResponseEntity.ok(
                LoginResponseDto.builder()
                        .token(jwtToken)
                        .build()
        );
    }

    @Operation(
            summary = "Request password reset",
            description = "Public endpoint to request a password reset link. Only processes requests for valid Client email addresses."
    )
    @ApiResponse(responseCode = "200", description = "If the email belongs to a client, a reset link was dispatched")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., invalid email format)")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        log.info("API POST request to initiate password reset");

        clientService.requestPasswordReset(request);

        // We return 200 OK even if the email doesn't exist to prevent email enumeration attacks
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Execute password reset",
            description = "Public endpoint to set a new password using a secure, time-bound reset token."
    )
    @ApiResponse(responseCode = "200", description = "Password successfully reset")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., token expired, missing fields)")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        log.info("API POST request to execute password reset with token");

        clientService.resetPassword(request);

        return ResponseEntity.ok().build();
    }
}
