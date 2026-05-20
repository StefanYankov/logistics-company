package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shipment.dto.*;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Core service contract orchestrating the lifecycle, validation, and retrieval of logistics shipments.
 */
public interface ShipmentService {

    /**
     * Registers a new shipment into the system.
     * Calculates pricing based on the current active configuration and destination type.
     * Generates a unique tracking number and logs the initial REGISTERED status history.
     *
     * @param request The data transfer object containing sender, receiver, dimensions, and destination.
     * @param registeredById The UUID of the currently authenticated employee (Clerk or Courier) registering the shipment.
     * @return The fully populated StaffShipmentViewDto of the new shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if any participant (Sender, Receiver, Employee) is not found,
     *                                                           or if the destination (Office) is invalid.
     */
    StaffShipmentViewDto registerShipment(ShipmentCreationDto request, UUID registeredById);

    /**
     * Updates an existing shipment's details.
     * Enforces business rules: shipment must be in REGISTERED status.
     * Clients can only update their own shipments.
     *
     * @param shipmentId The UUID of the shipment to update.
     * @param dto The data transfer object containing the updated fields.
     * @param userDetails The authenticated user performing the update.
     * @return The fully populated StaffShipmentViewDto of the updated shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if shipment is not found, not in REGISTERED status, or if validation fails.
     */
    StaffShipmentViewDto updateShipment(UUID shipmentId, ShipmentUpdateDto dto, CustomUserDetails userDetails);

    /**
     * Updates the lifecycle status of an existing shipment.
     * Enforces directional state machine rules to prevent invalid transitions (e.g., REGISTERED directly to DELIVERED).
     * Records the status change in the ShipmentStatusHistory.
     *
     * @param shipmentId The UUID of the shipment to update.
     * @param request The DTO containing the new status, optional location office, and optional notes.
     * @param userDetails The authenticated user performing the update. Used for role-based authorization and assigning the delivery courier.
     * @return The updated StaffShipmentViewDto.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the transition is invalid, the shipment is not found, or the location office is invalid.
     */
    StaffShipmentViewDto updateShipmentStatus(UUID shipmentId, ShipmentStatusUpdateDto request, CustomUserDetails userDetails);

    /**
     * Assigns a courier to a shipment for pickup from a client's address.
     * Enforces business rules: shipment must be REGISTERED and originate from an address.
     *
     * @param shipmentId The UUID of the shipment to assign.
     * @param courierId The UUID of the courier to assign.
     * @param userDetails The authenticated user performing the assignment (must be ADMIN or CLERK).
     * @return The updated StaffShipmentViewDto.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if shipment/courier not found, or business rules violated.
     */
    StaffShipmentViewDto assignPickup(UUID shipmentId, UUID courierId, CustomUserDetails userDetails);

    /**
     * Retrieves a specific shipment by its UUID for staff or authorized clients.
     * Enforces strict visibility rules:
     * - Admins, Clerks, and Couriers can view any shipment.
     * - Clients can only view the shipment if they are explicitly the Sender or Receiver.
     *
     * @param shipmentId The UUID of the shipment to retrieve.
     * @param requestingUserId The UUID of the authenticated user requesting the data.
     * @param role The role of the authenticated user to evaluate access logic.
     * @return The StaffShipmentViewDto of the shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the shipment is not found, or if the user is a Client
     *                                                           attempting to access another client's shipment (returns 404 to prevent enumeration).
     */
    StaffShipmentViewDto getShipmentById(UUID shipmentId, UUID requestingUserId, ApplicationRole role);

    /**
     * Retrieves a specific shipment by its UUID for staff or authorized clients.
     * This method is intended for internal staff use or authenticated clients who need full details.
     *
     * @param shipmentId The UUID of the shipment to retrieve.
     * @param requestingUserId The UUID of the authenticated user requesting the data.
     * @param role The role of the authenticated user to evaluate access logic.
     * @return The StaffShipmentViewDto of the shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the shipment is not found, or if the user is a Client
     *                                                           attempting to access another client's shipment (returns 404 to prevent enumeration).
     */
    StaffShipmentViewDto getStaffShipmentDetails(UUID shipmentId, UUID requestingUserId, ApplicationRole role);


    /**
     * Retrieves a specific shipment by its public tracking number for anonymous or authenticated users.
     * Returns a restricted PublicShipmentViewDto to protect sensitive information.
     *
     * @param trackingNumber The public alphanumeric tracking identifier.
     * @return The PublicShipmentViewDto of the shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the shipment is not found or access is unauthorized.
     */
    PublicShipmentViewDto getShipmentByTrackingNumber(String trackingNumber);

    /**
     * Retrieves a paginated list of all shipments assigned to a specific courier for delivery.
     * (Satisfies Requirement 4)
     *
     * @param courierId The UUID of the courier.
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getMyDeliveries(UUID courierId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments assigned to a specific courier for pickup.
     * (Satisfies Requirement 4)
     *
     * @param courierId The UUID of the courier.
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getMyPickups(UUID courierId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments sent by a specific client.
     * (Satisfies Requirement 5.f)
     *
     * @param senderId The UUID of the sending client.
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getShipmentsBySender(UUID senderId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments received by a specific client.
     * (Satisfies Requirement 5.g)
     *
     * @param receiverId The UUID of the receiving client.
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getShipmentsByReceiver(UUID receiverId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments registered into the system by a specific employee.
     * (Satisfies Requirement 5.d)
     *
     * @param employeeId The UUID of the employee (Clerk or Courier).
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getShipmentsRegisteredByEmployee(UUID employeeId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments that are currently not in a DELIVERED state.
     * (Satisfies Requirement 5.e: Shipments sent but not received)
     *
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getPendingShipments(Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments in the system.
     * Intended strictly for administrative and staff overview reporting.
     * (Satisfies Requirement 5.c)
     *
     * @param pageable Pagination and sorting criteria.
     * @return A page of StaffShipmentViewDto.
     */
    Page<StaffShipmentViewDto> getAllShipments(Pageable pageable);

    /**
     * Calculates the total revenue of the company for a specific date range.
     * Evaluates all registered shipments whose creation date falls within the boundaries.
     * (Satisfies Requirement 5.h)
     *
     * @param startDate The inclusive start date of the reporting period.
     * @param endDate The inclusive end date of the reporting period.
     * @return A report DTO containing the dates and the calculated total sum in BGN.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if startDate is strictly after endDate.
     */
    RevenueReportDto getCompanyRevenue(LocalDate startDate, LocalDate endDate);
}