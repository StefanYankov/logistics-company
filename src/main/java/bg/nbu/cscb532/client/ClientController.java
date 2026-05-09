package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for managing public Client operations.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/clients", produces = "application/json")
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Client API", description = "Endpoints for managing logistics clients.")
public class ClientController {

    private final ClientService clientService;

    @Operation(
            summary = "Register a new client",
            description = "Public endpoint to register a new client account. Returns 201 Created upon success."
    )
    @ApiResponse(responseCode = "201", description = "Client registered successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., missing fields or invalid email/phone format)")
    @ApiResponse(responseCode = "409", description = "Conflict - Username or email is already taken")
    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<ClientViewDto> registerClient(@Valid @RequestBody ClientRegistrationDto dto) {
        log.info("API POST request to register a new client with username: {}", dto.username());

        ClientViewDto createdClient = clientService.register(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/clients/{id}")
                .buildAndExpand(createdClient.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdClient);
    }

    @Operation(
            summary = "Verify client email",
            description = "Public endpoint to verify a client's email address using a secure token provided via email link."
    )
    @ApiResponse(responseCode = "200", description = "Email successfully verified and account activated")
    @ApiResponse(responseCode = "400", description = "Validation failed - Token is missing, invalid, or expired")
    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        log.info("API GET request to verify email with token");

        clientService.verifyEmail(token);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get all clients",
            description = "Retrieves a paginated list of all registered clients. Restricted to administrators."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ClientViewDto>> getAllClients(Pageable pageable) {
        log.info("API GET request for all clients. Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<ClientViewDto> clients = clientService.getAllClients(pageable);
        
        return ResponseEntity.ok(clients);
    }

    @Operation(
            summary = "Search clients",
            description = "Searches for clients by partial match on first name, last name, or phone number. Restricted to staff."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'COURIER')")
    public ResponseEntity<Page<ClientViewDto>> searchClients(
            @Parameter(description = "The search term (name or phone number)") @RequestParam String term,
            Pageable pageable) {
        log.info("API GET request to search clients with term: {}", term);

        Page<ClientViewDto> clients = clientService.searchClients(term, pageable);

        return ResponseEntity.ok(clients);
    }
}
