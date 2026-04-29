package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.ShipmentViewDto;
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
     * @return The fully populated view DTO of the new shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if any participant (Sender, Receiver, Employee) is not found,
     *                                                           or if the destination (Office) is invalid.
     */
    ShipmentViewDto registerShipment(ShipmentCreationDto request, UUID registeredById);

    /**
     * Updates the lifecycle status of an existing shipment.
     * Enforces directional state machine rules to prevent invalid transitions (e.g., REGISTERED directly to DELIVERED).
     * Records the status change in the ShipmentStatusHistory.
     *
     * @param shipmentId The UUID of the shipment to update.
     * @param request The DTO containing the new status, optional location office, and optional notes.
     * @param userDetails The authenticated user performing the update. Used for role-based authorization and assigning the delivery courier.
     * @return The updated shipment view DTO.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the transition is invalid, the shipment is not found, or the location office is invalid.
     */
    ShipmentViewDto updateShipmentStatus(UUID shipmentId, ShipmentStatusUpdateDto request, CustomUserDetails userDetails);

    /**
     * Retrieves a specific shipment by its UUID.
     * Enforces strict visibility rules:
     * - Admins, Clerks, and Couriers can view any shipment.
     * - Clients can only view the shipment if they are explicitly the Sender or Receiver.
     *
     * @param shipmentId The UUID of the shipment to retrieve.
     * @param requestingUserId The UUID of the authenticated user requesting the data.
     * @param role The role of the authenticated user to evaluate access logic.
     * @return The view DTO of the shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the shipment is not found, or if the user is a Client
     *                                                           attempting to access another client's shipment (returns 404 to prevent enumeration).
     */
    ShipmentViewDto getShipmentById(UUID shipmentId, UUID requestingUserId, ApplicationRole role);

    /**
     * Retrieves a specific shipment by its public tracking number.
     * Enforces strict visibility rules identical to UUID-based retrieval to prevent tracking number enumeration by malicious clients.
     *
     * @param trackingNumber The public alphanumeric tracking identifier.
     * @param requestingUserId The UUID of the authenticated user requesting the data.
     * @param role The role of the authenticated user to evaluate access logic.
     * @return The view DTO of the shipment.
     * @throws bg.nbu.cscb532.shared.exception.BusinessException if the shipment is not found or access is unauthorized.
     */
    ShipmentViewDto getShipmentByTrackingNumber(String trackingNumber, UUID requestingUserId, ApplicationRole role);

    /**
     * Retrieves a paginated list of all shipments sent by a specific client.
     * (Satisfies Requirement 5.f)
     *
     * @param senderId The UUID of the sending client.
     * @param pageable Pagination and sorting criteria.
     * @return A page of shipment view DTOs.
     */
    Page<ShipmentViewDto> getShipmentsBySender(UUID senderId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments received by a specific client.
     * (Satisfies Requirement 5.g)
     *
     * @param receiverId The UUID of the receiving client.
     * @param pageable Pagination and sorting criteria.
     * @return A page of shipment view DTOs.
     */
    Page<ShipmentViewDto> getShipmentsByReceiver(UUID receiverId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments registered into the system by a specific employee.
     * (Satisfies Requirement 5.d)
     *
     * @param employeeId The UUID of the employee (Clerk or Courier).
     * @param pageable Pagination and sorting criteria.
     * @return A page of shipment view DTOs.
     */
    Page<ShipmentViewDto> getShipmentsRegisteredByEmployee(UUID employeeId, Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments that are currently not in a DELIVERED state.
     * (Satisfies Requirement 5.e: Shipments sent but not received)
     *
     * @param pageable Pagination and sorting criteria.
     * @return A page of shipment view DTOs.
     */
    Page<ShipmentViewDto> getPendingShipments(Pageable pageable);

    /**
     * Retrieves a paginated list of all shipments in the system.
     * Intended strictly for administrative and staff overview reporting.
     * (Satisfies Requirement 5.c)
     *
     * @param pageable Pagination and sorting criteria.
     * @return A page of shipment view DTOs.
     */
    Page<ShipmentViewDto> getAllShipments(Pageable pageable);

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
