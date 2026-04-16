package bg.nbu.cscb532.client;

import bg.nbu.cscb532.client.dto.ClientRegistrationDto;
import bg.nbu.cscb532.client.dto.ClientViewDto;
import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for managing public Client operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
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
    @PostMapping("/register")
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
}
