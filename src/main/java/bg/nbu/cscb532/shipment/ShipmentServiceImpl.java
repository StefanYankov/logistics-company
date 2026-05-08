package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
import bg.nbu.cscb532.employee.Courier;
import bg.nbu.cscb532.employee.Employee;
import bg.nbu.cscb532.employee.EmployeeRepository;
import bg.nbu.cscb532.office.City;
import bg.nbu.cscb532.office.CityRepository;
import bg.nbu.cscb532.office.Office;
import bg.nbu.cscb532.office.OfficeRepository;
import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.exception.BusinessException;
import bg.nbu.cscb532.shared.exception.ErrorCode;
import bg.nbu.cscb532.shared.location.AddressDetails;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.dto.RevenueReportDto;
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.ShipmentStatusUpdateDto;
import bg.nbu.cscb532.shipment.dto.ShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the core Shipment Service.
 * Orchestrates the creation and retrieval of shipments, enforcing business rules and pricing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentStatusHistoryRepository historyRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;
    private final CityRepository cityRepository;
    private final PricingService pricingService;

    @Override
    @Transactional
    public ShipmentViewDto registerShipment(ShipmentCreationDto request, UUID registeredById) {
        log.debug("Attempting to register a new shipment for Sender ID: {}", request.senderId());

        Objects.requireNonNull(request, Constants.DeveloperErrors.DTO_NULL);
        Objects.requireNonNull(registeredById, Constants.DeveloperErrors.ENTITY_ID_NULL);

        Client sender = clientRepository.findById(request.senderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // --- XOR Validation for Receiver ---
        boolean hasReceiverId = request.receiverId() != null;
        boolean hasGuestDetails = request.receiverName() != null && !request.receiverName().isBlank() &&
                                  request.receiverPhone() != null && !request.receiverPhone().isBlank();

        if (hasReceiverId == hasGuestDetails) {
            throw new BusinessException(ErrorCode.SHIPMENT_RECEIVER_EXCLUSIVE);
        }

        Client receiver = null;
        if (hasReceiverId) {
            receiver = clientRepository.findById(request.receiverId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        // TODO: once the user is registered map by mobile phone maybe or whatever would be a good reason.
        // For example, if a guest receiver later registers an account with the same phone number,
        // run a batch job to link their old guest shipments to their new client ID.

        Employee registeredBy = employeeRepository.findById(registeredById)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // --- XOR Validation for Origin ---
        boolean hasOriginOfficeId = request.originOfficeId() != null;
        boolean hasOriginAddress = request.originAddress() != null;

        if (hasOriginOfficeId == hasOriginAddress) {
            // Re-using the same error code conceptually, though a new one could be created
            throw new BusinessException(ErrorCode.SHIPMENT_DESTINATION_EXCLUSIVE);
        }

        Office originOffice = null;
        AddressDetails originAddressSnapshot = null;

        if (hasOriginOfficeId) {
            originOffice = officeRepository.findById(request.originOfficeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));
        } else {
            originAddressSnapshot = buildAddressDetails(request.originAddress());
        }

        // --- XOR Validation for Destination ---
        boolean hasDeliveryOfficeId = request.deliveryOfficeId() != null;
        boolean hasDeliveryAddress = request.deliveryAddress() != null;

        if (hasDeliveryOfficeId == hasDeliveryAddress) {
            throw new BusinessException(ErrorCode.SHIPMENT_DESTINATION_EXCLUSIVE);
        }

        Office deliveryOffice = null;
        AddressDetails deliveryAddressSnapshot = null;

        if (hasDeliveryOfficeId) {
            deliveryOffice = officeRepository.findById(request.deliveryOfficeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));
        } else {
            deliveryAddressSnapshot = buildAddressDetails(request.deliveryAddress());
        }

        BigDecimal totalPrice = pricingService.calculatePrice(request);

        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PackageDetails packageDetails = PackageDetails.builder()
                .type(request.type())
                .weight(request.weight())
                .length(request.length())
                .width(request.width())
                .height(request.height())
                .build();

        ShipmentFinancials financials = ShipmentFinancials.builder()
                .totalPrice(totalPrice)
                .paidBy(request.paidBy())
                .isPaid(false) // Default to unpaid upon registration
                .build();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .sender(sender)
                .receiver(receiver)
                .receiverName(request.receiverName())
                .receiverPhone(request.receiverPhone())
                .receiverEmail(request.receiverEmail())
                .registeredBy(registeredBy)
                .packageDetails(packageDetails)
                .financials(financials)
                .status(ShipmentStatus.REGISTERED)
                .originOffice(originOffice)
                .originAddressSnapshot(originAddressSnapshot)
                .deliveryOffice(deliveryOffice)
                .deliveryAddressSnapshot(deliveryAddressSnapshot)
                .build();

        Shipment savedShipment = shipmentRepository.save(shipment);

        ShipmentStatusHistory initialHistory = ShipmentStatusHistory.builder()
                .shipment(savedShipment)
                .status(ShipmentStatus.REGISTERED)
                .notes("Shipment officially registered into the system.")
                .build();
        
        historyRepository.save(initialHistory);

        log.info("Successfully registered Shipment with Tracking Number: {}", trackingNumber);

        return mapToViewDto(savedShipment);
    }

    @Override
    @Transactional
    public ShipmentViewDto updateShipmentStatus(UUID shipmentId, ShipmentStatusUpdateDto request, CustomUserDetails userDetails) {
        log.debug("User {} attempting to update status for shipment {}", userDetails.getId(), shipmentId);

        Objects.requireNonNull(request, Constants.DeveloperErrors.DTO_NULL);
        Objects.requireNonNull(userDetails, Constants.DeveloperErrors.DTO_NULL);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        validateStatusTransition(shipment.getStatus(), request.newStatus());

        if (request.newStatus() == ShipmentStatus.DELIVERED || request.newStatus() == ShipmentStatus.OUT_FOR_DELIVERY) {
            if (userDetails.getApplicationRole() != ApplicationRole.COURIER) {
                log.warn("Non-courier user {} attempted to mark shipment {} as {}", userDetails.getId(), shipmentId, request.newStatus());
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            if (request.newStatus() == ShipmentStatus.DELIVERED) {
                Employee employee = employeeRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
                shipment.setDeliveredBy((Courier) employee);
            }
            
            // If out for delivery, set the current courier
            if (request.newStatus() == ShipmentStatus.OUT_FOR_DELIVERY) {
                 Employee employee = employeeRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
                 shipment.setCurrentCourier((Courier) employee);
                 shipment.setCurrentOffice(null); // It's no longer at an office
            }
        }

        Office locationOffice = null;
        if (request.locationOfficeId() != null) {
            locationOffice = officeRepository.findById(request.locationOfficeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));
                    
            // Update current location if provided
            if (request.newStatus() == ShipmentStatus.AT_DELIVERY_OFFICE || request.newStatus() == ShipmentStatus.IN_TRANSIT) {
                shipment.setCurrentOffice(locationOffice);
                shipment.setCurrentCourier(null);
            }
        }

        shipment.setStatus(request.newStatus());
        Shipment updatedShipment = shipmentRepository.save(shipment);

        ShipmentStatusHistory history = ShipmentStatusHistory.builder()
                .shipment(updatedShipment)
                .status(request.newStatus())
                .location(locationOffice)
                .notes(request.notes())
                .build();
        historyRepository.save(history);

        log.info("Shipment {} successfully updated to status {}", shipmentId, request.newStatus());

        return mapToViewDto(updatedShipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentViewDto getShipmentById(UUID shipmentId, UUID requestingUserId, ApplicationRole role) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (role == ApplicationRole.CLIENT) {
            boolean isSender = shipment.getSender().getId().equals(requestingUserId);
            boolean isReceiver = shipment.getReceiver() != null && shipment.getReceiver().getId().equals(requestingUserId);
            if (!isSender && !isReceiver) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        return mapToViewDto(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentViewDto getShipmentByTrackingNumber(String trackingNumber, UUID requestingUserId, ApplicationRole role) {
        log.debug("Looking up shipment by tracking number: {}", trackingNumber);

        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (role == ApplicationRole.CLIENT) {
            boolean isSender = shipment.getSender().getId().equals(requestingUserId);
            boolean isReceiver = shipment.getReceiver() != null && shipment.getReceiver().getId().equals(requestingUserId);
            if (!isSender && !isReceiver) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        return mapToViewDto(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentViewDto> getShipmentsBySender(UUID senderId, Pageable pageable) {
        return shipmentRepository.findBySender_Id(senderId, pageable)
                .map(this::mapToViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentViewDto> getShipmentsByReceiver(UUID receiverId, Pageable pageable) {
        return shipmentRepository.findByReceiver_Id(receiverId, pageable)
                .map(this::mapToViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentViewDto> getShipmentsRegisteredByEmployee(UUID employeeId, Pageable pageable) {
        return shipmentRepository.findByRegisteredBy_Id(employeeId, pageable)
                .map(this::mapToViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentViewDto> getPendingShipments(Pageable pageable) {
        return shipmentRepository.findByStatusNot(ShipmentStatus.DELIVERED, pageable)
                .map(this::mapToViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentViewDto> getAllShipments(Pageable pageable) {
        return shipmentRepository.findAll(pageable)
                .map(this::mapToViewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportDto getCompanyRevenue(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating total revenue from {} to {}", startDate, endDate);

        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        // Convert Calendar dates to precise Universal Time boundaries (UTC)
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.atTime(23, 59, 59, 999999999).toInstant(ZoneOffset.UTC);

        BigDecimal rawRevenue = shipmentRepository.calculateTotalRevenue(startInstant, endInstant);

        BigDecimal totalRevenue = rawRevenue != null ? rawRevenue : BigDecimal.ZERO;

        return RevenueReportDto.builder()
                .totalRevenue(totalRevenue)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // --- Private Helper Methods ---

    private void validateStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        boolean isValid = switch (currentStatus) {
            case REGISTERED -> newStatus == ShipmentStatus.IN_TRANSIT;
            case IN_TRANSIT -> newStatus == ShipmentStatus.AT_DELIVERY_OFFICE || newStatus == ShipmentStatus.OUT_FOR_DELIVERY;
            case AT_DELIVERY_OFFICE -> newStatus == ShipmentStatus.OUT_FOR_DELIVERY || newStatus == ShipmentStatus.DELIVERED;
            case OUT_FOR_DELIVERY -> newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.AT_DELIVERY_OFFICE;
            case DELIVERED -> false;
        };

        if (!isValid) {
            log.warn("Invalid status transition attempted: {} -> {}", currentStatus, newStatus);
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private AddressDetails buildAddressDetails(AddressDetailsDto dto) {
        City city = cityRepository.findById(dto.cityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CITY_NOT_FOUND));

        return AddressDetails.builder()
                .city(city)
                .street(dto.street().trim())
                .district(dto.district() != null ? dto.district().trim() : null)
                .building(dto.building() != null ? dto.building().trim() : null)
                .entrance(dto.entrance() != null ? dto.entrance().trim() : null)
                .floor(dto.floor() != null ? dto.floor().trim() : null)
                .apartment(dto.apartment() != null ? dto.apartment().trim() : null)
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .build();
    }

    private ShipmentViewDto mapToViewDto(Shipment shipment) {
        if (shipment == null) return null;

        Long originOfficeId = null;
        String originOfficeName = null;
        String originAddressString = null;
        
        if (shipment.getOriginOffice() != null) {
             originOfficeId = shipment.getOriginOffice().getId();
             originOfficeName = shipment.getOriginOffice().getAddressDetails().getCity().getName() + " - " + 
                                shipment.getOriginOffice().getAddressDetails().getStreet();
        } else if (shipment.getOriginAddressSnapshot() != null) {
             originAddressString = formatAddress(shipment.getOriginAddressSnapshot());
        }

        Long deliveryOfficeId = null;
        String deliveryOfficeName = null;
        String deliveryAddressString = null;

        if (shipment.getDeliveryOffice() != null) {
            deliveryOfficeId = shipment.getDeliveryOffice().getId();
            deliveryOfficeName = shipment.getDeliveryOffice().getAddressDetails().getCity().getName() + " - " +
                                 shipment.getDeliveryOffice().getAddressDetails().getStreet();
        } else if (shipment.getDeliveryAddressSnapshot() != null) {
            deliveryAddressString = formatAddress(shipment.getDeliveryAddressSnapshot());
        }

        String senderName = shipment.getSender().getFirstName() + " " + shipment.getSender().getLastName();
        
        // Handle guest vs registered receiver
        String receiverName;
        String receiverPhone;
        UUID receiverId = null;
        if (shipment.getReceiver() != null) {
            receiverName = shipment.getReceiver().getFirstName() + " " + shipment.getReceiver().getLastName();
            receiverPhone = shipment.getReceiver().getPhoneNumber();
            receiverId = shipment.getReceiver().getId();
        } else {
            receiverName = shipment.getReceiverName();
            receiverPhone = shipment.getReceiverPhone();
        }
        
        Long currentOfficeId = shipment.getCurrentOffice() != null ? shipment.getCurrentOffice().getId() : null;
        String currentOfficeName = shipment.getCurrentOffice() != null ? 
            shipment.getCurrentOffice().getAddressDetails().getCity().getName() + " - " + shipment.getCurrentOffice().getAddressDetails().getStreet() : null;
            
        UUID currentCourierId = shipment.getCurrentCourier() != null ? shipment.getCurrentCourier().getId() : null;
        String currentCourierName = shipment.getCurrentCourier() != null ? 
            shipment.getCurrentCourier().getFirstName() + " " + shipment.getCurrentCourier().getLastName() : null;

        String registeredByName = shipment.getRegisteredBy() != null ? 
                shipment.getRegisteredBy().getFirstName() + " " + shipment.getRegisteredBy().getLastName() : "Self-Registered";

        return ShipmentViewDto.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .type(shipment.getPackageDetails().getType())
                .status(shipment.getStatus())
                .weight(shipment.getPackageDetails().getWeight())
                .length(shipment.getPackageDetails().getLength())
                .width(shipment.getPackageDetails().getWidth())
                .height(shipment.getPackageDetails().getHeight())
                .totalPrice(shipment.getFinancials().getTotalPrice())
                .paidBy(shipment.getFinancials().getPaidBy())
                .isPaid(shipment.getFinancials().isPaid())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .senderId(shipment.getSender().getId())
                .senderName(senderName.trim())
                .senderPhone(shipment.getSender().getPhoneNumber())
                .receiverId(receiverId)
                .receiverName(receiverName.trim())
                .receiverPhone(receiverPhone)
                .originOfficeId(originOfficeId)
                .originOfficeName(originOfficeName)
                .originAddressString(originAddressString)
                .deliveryOfficeId(deliveryOfficeId)
                .deliveryOfficeName(deliveryOfficeName)
                .deliveryAddressString(deliveryAddressString)
                .currentOfficeId(currentOfficeId)
                .currentOfficeName(currentOfficeName)
                .currentCourierId(currentCourierId)
                .currentCourierName(currentCourierName)
                .registeredById(shipment.getRegisteredBy() != null ? shipment.getRegisteredBy().getId() : null)
                .registeredByName(registeredByName.trim())
                .build();
    }

    private String formatAddress(AddressDetails address) {
        if (address == null) {
            return "";
        }

        return Stream.of(
                        address.getStreet(),
                        address.getDistrict(),
                        formatOptionalPart("bl. ", address.getBuilding()),
                        formatOptionalPart("ent. ", address.getEntrance()),
                        formatOptionalPart("fl. ", address.getFloor()),
                        formatOptionalPart("ap. ", address.getApartment()),
                        address.getCity().getName() + " " + address.getCity().getPostcode()
                )
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String formatOptionalPart(String prefix, String value) {
        if (value != null && !value.isBlank()) {
            return prefix + value;
        }
        return null;
    }
}
