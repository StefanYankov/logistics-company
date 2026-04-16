package bg.nbu.cscb532.user;

import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import bg.nbu.cscb532.user.dto.LoginRequestDto;
import bg.nbu.cscb532.user.dto.LoginResponseDto;
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
}
