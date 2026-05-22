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
import bg.nbu.cscb532.shipment.dto.*;
import bg.nbu.cscb532.shipment.dto.mapper.ShipmentMapper;
import bg.nbu.cscb532.user.ApplicationRole;
import bg.nbu.cscb532.user.CustomUserDetails;
import bg.nbu.cscb532.user.User;
import bg.nbu.cscb532.user.UserRepository;
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
import java.util.*;
import java.util.stream.Collectors;

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
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ShipmentAddonRepository shipmentAddonRepository;
    private final ShipmentMapper shipmentMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public StaffShipmentViewDto registerShipment(ShipmentCreationDto request, UUID registeredById) {
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
        } else {
            // Auto-Match Receivers
            Optional<Client> matchedClientOpt = clientRepository.findByPhoneNumber(request.receiverPhone().trim());
            if (matchedClientOpt.isPresent()) {
                receiver = matchedClientOpt.get();
                log.info("Auto-matched guest receiver phone [{}] to existing client ID: {}", request.receiverPhone(), receiver.getId());
            }
        }

        User registeringUser = userRepository.findById(registeredById)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Employee registeredBy = null;
        if (registeringUser.getApplicationRole() == ApplicationRole.CLERK || registeringUser.getApplicationRole() == ApplicationRole.ADMIN) {
            registeredBy = employeeRepository.findById(registeredById)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        }

        // --- XOR Validation for Origin ---
        boolean hasOriginOfficeId = request.originOfficeId() != null;
        boolean hasOriginAddress = request.originAddress() != null;

        if (hasOriginOfficeId == hasOriginAddress) {
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
                .isPaid(false)
                .build();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .sender(sender)
                .receiver(receiver)
                .receiverName(receiver == null ? request.receiverName() : null)
                .receiverPhone(receiver == null ? request.receiverPhone() : null)
                .receiverEmail(receiver == null ? request.receiverEmail() : null)
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

        // Process Addons
        if (request.selectedServiceIds() != null && !request.selectedServiceIds().isEmpty()) {
            Set<ServiceCatalog> selectedServices = new HashSet<>(serviceCatalogRepository.findAllById(request.selectedServiceIds()));

            if (selectedServices.size() != request.selectedServiceIds().size()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }

            for (ServiceCatalog service : selectedServices) {
                ShipmentAddon addon = new ShipmentAddon();
                addon.setShipment(savedShipment);
                addon.setServiceCatalog(service);

                BigDecimal appliedCost = BigDecimal.ZERO;
                if (service.getPricingType() == PricingType.FIXED_AMOUNT) {
                    appliedCost = service.getPricingValue();
                } else if (service.getPricingType() == PricingType.PERCENTAGE_OF_BASE) {
                    BigDecimal surcharge = totalPrice.multiply(service.getPricingValue());
                    appliedCost = surcharge;
                }
                addon.setAppliedCost(appliedCost);
                shipmentAddonRepository.save(addon);
            }
        }

        ShipmentStatusHistory initialHistory = ShipmentStatusHistory.builder()
                .shipment(savedShipment)
                .status(ShipmentStatus.REGISTERED)
                .notes("Shipment officially registered into the system.")
                .build();

        historyRepository.save(initialHistory);

        // TODO: Implement a geographic or load-balanced auto-assignment algorithm for address pickups.
        log.info("Successfully registered Shipment with Tracking Number: {}", trackingNumber);

        return shipmentMapper.toStaffView(savedShipment);
    }

    @Override
    @Transactional
    public StaffShipmentViewDto updateShipment(UUID shipmentId, ShipmentUpdateDto dto, CustomUserDetails userDetails) {
        log.debug("User {} attempting to update shipment {}", userDetails.getId(), shipmentId);

        Objects.requireNonNull(dto, Constants.DeveloperErrors.DTO_NULL);
        Objects.requireNonNull(userDetails, Constants.DeveloperErrors.DTO_NULL);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (userDetails.getApplicationRole() == ApplicationRole.CLIENT) {
            if (!shipment.getSender().getId().equals(userDetails.getId())) {
                log.warn("Client {} attempted to edit shipment {} belonging to a different sender", userDetails.getId(), shipmentId);
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        if (shipment.getStatus() != ShipmentStatus.REGISTERED) {
            log.warn("Attempt to update shipment {} failed. Current status is {}. Only REGISTERED shipments can be edited.", shipmentId, shipment.getStatus());
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        if (dto.type() != null) {
            shipment.getPackageDetails().setType(dto.type());
        }
        if (dto.weight() != null) {
            shipment.getPackageDetails().setWeight(dto.weight());
        }
        if (dto.length() != null) shipment.getPackageDetails().setLength(dto.length());
        if (dto.width() != null) shipment.getPackageDetails().setWidth(dto.width());
        if (dto.height() != null) shipment.getPackageDetails().setHeight(dto.height());

        if (dto.receiverName() != null && !dto.receiverName().isBlank())
            shipment.setReceiverName(dto.receiverName());
        if (dto.receiverPhone() != null && !dto.receiverPhone().isBlank())
            shipment.setReceiverPhone(dto.receiverPhone());
        if (dto.receiverEmail() != null) shipment.setReceiverEmail(dto.receiverEmail());

        if (dto.paidBy() != null) shipment.getFinancials().setPaidBy(dto.paidBy());
        if (dto.isPaid() != null) shipment.getFinancials().setPaid(dto.isPaid());

        boolean hasDeliveryOfficeId = dto.deliveryOfficeId() != null;
        boolean hasDeliveryAddress = dto.deliveryAddress() != null;

        if (hasDeliveryOfficeId && hasDeliveryAddress) {
            throw new BusinessException(ErrorCode.SHIPMENT_DESTINATION_EXCLUSIVE);
        }

        if (hasDeliveryOfficeId) {
            Office deliveryOffice = officeRepository.findById(dto.deliveryOfficeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));
            shipment.setDeliveryOffice(deliveryOffice);
            shipment.setDeliveryAddressSnapshot(null);
        } else if (hasDeliveryAddress) {
            AddressDetails deliveryAddressSnapshot = buildAddressDetails(dto.deliveryAddress());
            shipment.setDeliveryAddressSnapshot(deliveryAddressSnapshot);
            shipment.setDeliveryOffice(null);
        }

        ShipmentCreationDto tempCreationDto = convertToCreationDtoForPricing(shipment, dto.selectedServiceIds());
        BigDecimal newTotalPrice = pricingService.calculatePrice(tempCreationDto);
        shipment.getFinancials().setTotalPrice(newTotalPrice);

        if (dto.selectedServiceIds() != null) {
            shipmentAddonRepository.deleteAll(shipment.getAddons());
            shipment.getAddons().clear();

            if (!dto.selectedServiceIds().isEmpty()) {
                Set<ServiceCatalog> selectedServices = new HashSet<>(serviceCatalogRepository.findAllById(dto.selectedServiceIds()));
                if (selectedServices.size() != dto.selectedServiceIds().size()) {
                    throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
                }

                for (ServiceCatalog service : selectedServices) {
                    ShipmentAddon addon = new ShipmentAddon();
                    addon.setShipment(shipment);
                    addon.setServiceCatalog(service);

                    BigDecimal appliedCost = BigDecimal.ZERO;
                    if (service.getPricingType() == PricingType.FIXED_AMOUNT) {
                        appliedCost = service.getPricingValue();
                    } else if (service.getPricingType() == PricingType.PERCENTAGE_OF_BASE) {
                        BigDecimal surcharge = newTotalPrice.multiply(service.getPricingValue());
                        appliedCost = surcharge;
                    }
                    addon.setAppliedCost(appliedCost);
                    shipment.getAddons().add(addon);
                    shipmentAddonRepository.save(addon);
                }
            }
        }

        Shipment savedShipment = shipmentRepository.save(shipment);

        ShipmentStatusHistory updateHistory = ShipmentStatusHistory.builder()
                .shipment(savedShipment)
                .status(ShipmentStatus.REGISTERED)
                .notes("Shipment details updated by " + userDetails.getApplicationRole().name())
                .build();
        historyRepository.save(updateHistory);

        log.info("Successfully updated Shipment with Tracking Number: {}", shipment.getTrackingNumber());

        return shipmentMapper.toStaffView(savedShipment);
    }

    /**
     * Helper method to map existing Shipment entity data to a ShipmentCreationDto
     * specifically for the purpose of feeding it to the PricingService.
     */
    private ShipmentCreationDto convertToCreationDtoForPricing(Shipment shipment, Set<Long> newServiceIds) {
        AddressDetailsDto deliveryAddressDto = null;
        if (shipment.getDeliveryAddressSnapshot() != null) {
            AddressDetails addr = shipment.getDeliveryAddressSnapshot();
            deliveryAddressDto = new AddressDetailsDto(
                    addr.getCity().getId(),
                    addr.getStreet(),
                    addr.getDistrict(),
                    addr.getBuilding(),
                    addr.getEntrance(),
                    addr.getFloor(),
                    addr.getApartment(),
                    addr.getLatitude(),
                    addr.getLongitude()
            );
        }

        return ShipmentCreationDto.builder()
                .senderId(shipment.getSender().getId())
                .receiverId(shipment.getReceiver() != null ? shipment.getReceiver().getId() : null)
                .type(shipment.getPackageDetails().getType())
                .weight(shipment.getPackageDetails().getWeight())
                .length(shipment.getPackageDetails().getLength())
                .width(shipment.getPackageDetails().getWidth())
                .height(shipment.getPackageDetails().getHeight())
                .paidBy(shipment.getFinancials().getPaidBy())
                .originOfficeId(shipment.getOriginOffice() != null ? shipment.getOriginOffice().getId() : null)
                .deliveryOfficeId(shipment.getDeliveryOffice() != null ? shipment.getDeliveryOffice().getId() : null)
                .deliveryAddress(deliveryAddressDto)
                .selectedServiceIds(newServiceIds != null ? newServiceIds : shipment.getAddons().stream().map(a -> a.getServiceCatalog().getId()).collect(Collectors.toSet()))
                .build();
    }


    @Override
    @Transactional
    public StaffShipmentViewDto updateShipmentStatus(UUID shipmentId, ShipmentStatusUpdateDto request, CustomUserDetails userDetails) {
        log.debug("User {} attempting to update status for shipment {}", userDetails.getId(), shipmentId);

        Objects.requireNonNull(request, Constants.DeveloperErrors.DTO_NULL);
        Objects.requireNonNull(userDetails, Constants.DeveloperErrors.DTO_NULL);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        validateStatusTransition(shipment.getStatus(), request.newStatus());

        if (request.newStatus() == ShipmentStatus.DELIVERED) {
            if (userDetails.getApplicationRole() == ApplicationRole.COURIER) {
                Employee employee = employeeRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
                shipment.setDeliveredBy((Courier) employee);
            } else if (userDetails.getApplicationRole() == ApplicationRole.CLERK) {
                if (shipment.getStatus() != ShipmentStatus.AT_DELIVERY_OFFICE && shipment.getStatus() != ShipmentStatus.DELIVERED) {
                    log.warn("Clerk user {} attempted to mark shipment {} as DELIVERED while it is not at an office (Current status: {})", userDetails.getId(), shipmentId, shipment.getStatus());
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED);
                }
            } else {
                log.warn("Unauthorized role {} attempted to mark shipment {} as DELIVERED", userDetails.getApplicationRole(), shipmentId);
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }

        if (request.newStatus() == ShipmentStatus.OUT_FOR_DELIVERY) {
            if (userDetails.getApplicationRole() != ApplicationRole.COURIER) {
                log.warn("Non-courier user {} attempted to mark shipment {} as OUT_FOR_DELIVERY", userDetails.getId(), shipmentId);
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            Employee employee = employeeRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
            shipment.setCurrentCourier((Courier) employee);
            shipment.setCurrentOffice(null);
        }

        Office locationOffice = null;
        if (request.locationOfficeId() != null) {
            locationOffice = officeRepository.findById(request.locationOfficeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));

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

        return shipmentMapper.toStaffView(updatedShipment);
    }

    @Override
    @Transactional
    public StaffShipmentViewDto assignPickup(UUID shipmentId, UUID courierId, CustomUserDetails userDetails) {
        log.debug("User {} attempting to assign pickup for shipment {} to courier {}", userDetails.getId(), shipmentId, courierId);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Employee employee = employeeRepository.findById(courierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!(employee instanceof Courier courier)) {
            log.warn("Attempted to assign pickup to an employee {} that is not a courier", courierId);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (shipment.getStatus() != ShipmentStatus.REGISTERED) {
            log.warn("Cannot assign pickup. Shipment {} is not in REGISTERED status (Current: {})", shipmentId, shipment.getStatus());
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        if (shipment.getOriginOffice() != null) {
            log.warn("Cannot assign pickup. Shipment {} originates from an office, not an address.", shipmentId);
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        shipment.setCurrentCourier(courier);
        Shipment updatedShipment = shipmentRepository.save(shipment);

        ShipmentStatusHistory history = ShipmentStatusHistory.builder()
                .shipment(updatedShipment)
                .status(ShipmentStatus.REGISTERED)
                .notes("Assigned to courier " + courier.getFirstName() + " " + courier.getLastName() + " for pickup.")
                .build();
        historyRepository.save(history);

        return shipmentMapper.toStaffView(updatedShipment);
    }


    @Override
    @Transactional(readOnly = true)
    public StaffShipmentViewDto getShipmentById(UUID shipmentId, UUID requestingUserId, ApplicationRole role) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (role == ApplicationRole.CLIENT) {
            boolean isSender = shipment.getSender().getId().equals(requestingUserId);
            boolean isReceiver = shipment.getReceiver() != null && shipment.getReceiver().getId().equals(requestingUserId);
            if (!isSender && !isReceiver) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        return shipmentMapper.toStaffView(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffShipmentViewDto getStaffShipmentDetails(UUID shipmentId, UUID requestingUserId, ApplicationRole role) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (role == ApplicationRole.CLIENT) {
            boolean isSender = shipment.getSender().getId().equals(requestingUserId);
            boolean isReceiver = shipment.getReceiver() != null && shipment.getReceiver().getId().equals(requestingUserId);
            if (!isSender && !isReceiver) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
        }

        return shipmentMapper.toStaffView(shipment);
    }


    @Override
    @Transactional(readOnly = true)
    public PublicShipmentViewDto getShipmentByTrackingNumber(String trackingNumber) {
        log.debug("Looking up shipment by tracking number: {}", trackingNumber);

        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return shipmentMapper.toPublicView(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getMyDeliveries(UUID courierId, Pageable pageable) {
        return shipmentRepository.findByCurrentCourier_IdAndStatus(courierId, ShipmentStatus.OUT_FOR_DELIVERY, pageable)
                .map(shipmentMapper::toStaffView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getMyPickups(UUID courierId, Pageable pageable) {
        return shipmentRepository.findByCurrentCourier_IdAndStatusAndOriginOfficeIsNull(courierId, ShipmentStatus.REGISTERED, pageable)
                .map(shipmentMapper::toStaffView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getShipmentsBySender(UUID senderId, Pageable pageable) {
        return shipmentRepository.findBySender_Id(senderId, pageable)
                .map(shipmentMapper::toStaffView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getShipmentsByReceiver(UUID receiverId, Pageable pageable) {
        return shipmentRepository.findByReceiver_Id(receiverId, pageable)
                .map(shipmentMapper::toStaffView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getShipmentsRegisteredByEmployee(UUID employeeId, Pageable pageable) {
        return shipmentRepository.findByRegisteredBy_Id(employeeId, pageable)
                .map(shipmentMapper::toStaffView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getPendingShipments(Pageable pageable) {
        return shipmentRepository.findByStatusNot(ShipmentStatus.DELIVERED, pageable)
                .map(shipmentMapper::toStaffView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffShipmentViewDto> getAllShipments(Pageable pageable) {
        return shipmentRepository.findAll(pageable)
                .map(shipmentMapper::toStaffView);
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

}
