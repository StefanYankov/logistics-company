package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shared.web.ApiStandardResponses;
import bg.nbu.cscb532.shipment.dto.*;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST Controller for managing the creation and retrieval of logistics shipments.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/shipments", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiStandardResponses
@RequiredArgsConstructor
@Tag(name = "Shipment API", description = "Endpoints for creating and tracking shipments.")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @Operation(
            summary = "Register a new shipment",
            description = "Creates a new shipment, calculates the price, and logs the initial REGISTERED status. Accessible to staff and clients."
    )
    @ApiResponse(responseCode = "201", description = "Shipment registered successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., negative weight, mutually exclusive destination error)")
    @ApiResponse(responseCode = "404", description = "Sender, Receiver, or delivery Office not found")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StaffShipmentViewDto> registerShipment(
            @Valid @RequestBody ShipmentCreationDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("API POST request to register a new shipment from user ID: {}", userDetails.getId());

        if (userDetails.getApplicationRole() == ApplicationRole.CLIENT) {
            if (!userDetails.getId().equals(request.senderId())) {
                log.warn("Client {} attempted to register a shipment with a different sender ID {}", userDetails.getId(), request.senderId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clients can only register shipments under their own ID.");
            }
        }

        StaffShipmentViewDto createdShipment = shipmentService.registerShipment(request, userDetails.getId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdShipment.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdShipment);
    }

    @Operation(
            summary = "Update shipment status",
            description = "Transitions a shipment through its lifecycle state machine. Automatically assigns delivery couriers and records location history."
    )
    @ApiResponse(responseCode = "200", description = "Shipment status updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., invalid state machine transition)")
    @ApiResponse(responseCode = "404", description = "Shipment or location office not found")
    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'COURIER')")
    public ResponseEntity<StaffShipmentViewDto> updateShipmentStatus(
            @Parameter(description = "The UUID of the shipment") @PathVariable UUID id,
            @Valid @RequestBody ShipmentStatusUpdateDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("API PATCH request to update status for shipment ID: {} from user ID: {}\n", id, userDetails.getId());

        StaffShipmentViewDto updatedShipment = shipmentService.updateShipmentStatus(id, request, userDetails);

        return ResponseEntity.ok(updatedShipment);
    }

    @Operation(
            summary = "Assign a courier for shipment pickup",
            description = "Assigns a specific courier to a REGISTERED shipment that originates from a client's address. Restricted to ADMIN and CLERK roles."
    )
    @ApiResponse(responseCode = "200", description = "Courier assigned successfully for pickup")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., shipment not REGISTERED, not an address pickup)")
    @ApiResponse(responseCode = "404", description = "Shipment or Courier not found")
    @PatchMapping(value = "/{id}/assign-pickup/{courierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<StaffShipmentViewDto> assignPickup(
            @Parameter(description = "The UUID of the shipment") @PathVariable UUID id,
            @Parameter(description = "The UUID of the courier to assign") @PathVariable UUID courierId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("API PATCH request to assign pickup for shipment ID: {} to courier ID: {} by user ID: {}", id, courierId, userDetails.getId());

        StaffShipmentViewDto updatedShipment = shipmentService.assignPickup(id, courierId, userDetails);
        return ResponseEntity.ok(updatedShipment);
    }

    @Operation(
            summary = "Get company revenue",
            description = "Calculates the total aggregate revenue across all shipments registered within a specified date range. Restricted to administrators."
    )
    @ApiResponse(responseCode = "200", description = "Revenue calculated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid date range provided")
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RevenueReportDto> getCompanyRevenue(
            @Parameter(description = "The start date (inclusive) in YYYY-MM-DD format") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "The end date (inclusive) in YYYY-MM-DD format") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("API GET request for company revenue from {} to {}", startDate, endDate);
        
        RevenueReportDto report = shipmentService.getCompanyRevenue(startDate, endDate);
        
        return ResponseEntity.ok(report);
    }

    @Operation(
            summary = "Get shipment by ID",
            description = "Retrieves a specific shipment. Clients can only view shipments where they are the sender or receiver."
    )
    @ApiResponse(responseCode = "200", description = "Shipment found and authorized")
    @ApiResponse(responseCode = "404", description = "Shipment not found or access denied")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StaffShipmentViewDto> getShipmentById(
            @Parameter(description = "The UUID of the shipment") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("API GET request for shipment ID: {} from user ID: {}", id, userDetails.getId());
        
        StaffShipmentViewDto shipment = shipmentService.getShipmentById(id, userDetails.getId(), userDetails.getApplicationRole());
        return ResponseEntity.ok(shipment);
    }

    @Operation(
            summary = "Get full shipment details by ID",
            description = "Retrieves the complete, uncensored details of a specific shipment. Restricted to staff roles or explicitly authorized clients."
    )
    @ApiResponse(responseCode = "200", description = "Shipment found and authorized")
    @ApiResponse(responseCode = "404", description = "Shipment not found or access denied")
    @GetMapping("/details/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'COURIER')")
    public ResponseEntity<StaffShipmentViewDto> getStaffShipmentDetails(
            @Parameter(description = "The UUID of the shipment") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("API GET request for full shipment details ID: {} from staff ID: {}", id, userDetails.getId());

        StaffShipmentViewDto shipment = shipmentService.getStaffShipmentDetails(id, userDetails.getId(), userDetails.getApplicationRole());
        return ResponseEntity.ok(shipment);
    }

    @Operation(
            summary = "Get shipment by tracking number",
            description = "Retrieves a specific shipment using its public tracking number. Accessible without authentication. Returns restricted PublicShipmentViewDto."
    )
    @ApiResponse(responseCode = "200", description = "Shipment found")
    @ApiResponse(responseCode = "400", description = "Validation failed (e.g., tracking number is blank)")
    @ApiResponse(responseCode = "404", description = "Shipment not found")
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<PublicShipmentViewDto> getShipmentByTrackingNumber(
            @Parameter(description = "The alphanumeric tracking number of the shipment") @PathVariable String trackingNumber) {
        
        log.info("API GET request for shipment tracking number: {} (Public)", trackingNumber);
        
        // Pass null for userDetails since it's a public endpoint
        PublicShipmentViewDto shipment = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(shipment);
    }

    @Operation(
            summary = "Get all shipments assigned to courier for delivery",
            description = "Retrieves a paginated list of all shipments that are currently assigned to the logged-in courier for delivery."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/my-deliveries")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<Page<StaffShipmentViewDto>> getMyDeliveries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        log.info("API GET request for my-deliveries from courier ID: {}", userDetails.getId());
        return ResponseEntity.ok(shipmentService.getMyDeliveries(userDetails.getId(), pageable));
    }

    @Operation(
            summary = "Get all shipments assigned to courier for pickup",
            description = "Retrieves a paginated list of all shipments that are currently assigned to the logged-in courier for pickup."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/my-pickups")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<Page<StaffShipmentViewDto>> getMyPickups(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        log.info("API GET request for my-pickups from courier ID: {}", userDetails.getId());
        return ResponseEntity.ok(shipmentService.getMyPickups(userDetails.getId(), pageable));
    }


    @Operation(
            summary = "Get all shipments",
            description = "Retrieves a paginated list of all shipments in the system. Restricted to staff roles."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'COURIER')")
    public ResponseEntity<Page<StaffShipmentViewDto>> getAllShipments(Pageable pageable) {
        log.info("API GET request for all shipments. Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(shipmentService.getAllShipments(pageable));
    }

    @Operation(
            summary = "Get shipments sent by a client",
            description = "Retrieves a paginated list of shipments where the specified client is the sender. Accessible to staff and the specific client."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/sender/{senderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<StaffShipmentViewDto>> getShipmentsBySender(
            @Parameter(description = "The UUID of the sending client") @PathVariable UUID senderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        log.info("API GET request for shipments sent by client ID: {} from user ID: {}", senderId, userDetails.getId());

        if (userDetails.getApplicationRole() == ApplicationRole.CLIENT) {
            if (!userDetails.getId().equals(senderId)) {
                log.warn("Client {} attempted to fetch sent shipments for ID {}", userDetails.getId(), senderId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
            }
        }

        return ResponseEntity.ok(shipmentService.getShipmentsBySender(senderId, pageable));
    }

    @Operation(
            summary = "Get shipments received by a client",
            description = "Retrieves a paginated list of shipments where the specified client is the receiver. Accessible to staff and the specific client."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/receiver/{receiverId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<StaffShipmentViewDto>> getShipmentsByReceiver(
            @Parameter(description = "The UUID of the receiving client") @PathVariable UUID receiverId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        log.info("API GET request for shipments received by client ID: {} from user ID: {}", receiverId, userDetails.getId());

        if (userDetails.getApplicationRole() == ApplicationRole.CLIENT) {
            if (!userDetails.getId().equals(receiverId)) {
                log.warn("Client {} attempted to fetch received shipments for ID {}", userDetails.getId(), receiverId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
            }
        }

        return ResponseEntity.ok(shipmentService.getShipmentsByReceiver(receiverId, pageable));
    }

    @Operation(
            summary = "Get pending shipments",
            description = "Retrieves a paginated list of all shipments that are not yet DELIVERED. Restricted to staff roles."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'COURIER')")
    public ResponseEntity<Page<StaffShipmentViewDto>> getPendingShipments(Pageable pageable) {
        log.info("API GET request for pending shipments.");
        return ResponseEntity.ok(shipmentService.getPendingShipments(pageable));
    }

    @Operation(
            summary = "Get shipments registered by an employee",
            description = "Retrieves a paginated list of shipments registered by a specific employee. Restricted to staff roles."
    )
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @GetMapping("/registered-by/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'COURIER')")
    public ResponseEntity<Page<StaffShipmentViewDto>> getShipmentsRegisteredByEmployee(
            @Parameter(description = "The UUID of the employee") @PathVariable UUID employeeId,
            Pageable pageable) {
        log.info("API GET request for shipments registered by employee ID: {}", employeeId);
        return ResponseEntity.ok(shipmentService.getShipmentsRegisteredByEmployee(employeeId, pageable));
    }
}
