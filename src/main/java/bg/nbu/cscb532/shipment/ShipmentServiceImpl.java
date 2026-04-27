package bg.nbu.cscb532.shipment;

import bg.nbu.cscb532.client.Client;
import bg.nbu.cscb532.client.ClientRepository;
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
import bg.nbu.cscb532.shipment.dto.ShipmentCreationDto;
import bg.nbu.cscb532.shipment.dto.ShipmentViewDto;
import bg.nbu.cscb532.user.ApplicationRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        
        Client receiver = clientRepository.findById(request.receiverId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Employee registeredBy = employeeRepository.findById(registeredById)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Office deliveryOffice = null;
        AddressDetails deliveryAddressSnapshot = null;

        if (request.deliveryOfficeId() != null && request.deliveryAddress() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } else if (request.deliveryOfficeId() != null) {
            deliveryOffice = officeRepository.findById(request.deliveryOfficeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OFFICE_NOT_FOUND));
        } else if (request.deliveryAddress() != null) {
            deliveryAddressSnapshot = buildAddressDetails(request.deliveryAddress());
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        BigDecimal totalPrice = pricingService.calculatePrice(request);

        String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .sender(sender)
                .receiver(receiver)
                .registeredBy(registeredBy)
                .type(request.type())
                .weight(request.weight())
                .length(request.length())
                .width(request.width())
                .height(request.height())
                .totalPrice(totalPrice)
                .status(ShipmentStatus.REGISTERED)
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
    @Transactional(readOnly = true)
    public ShipmentViewDto getShipmentById(UUID shipmentId, UUID requestingUserId, ApplicationRole role) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (role == ApplicationRole.CLIENT) {
            if (!shipment.getSender().getId().equals(requestingUserId) &&
                !shipment.getReceiver().getId().equals(requestingUserId)) {
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

    // --- Private Helper Methods ---

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
        String receiverName = shipment.getReceiver().getFirstName() + " " + shipment.getReceiver().getLastName();
        String registeredByName = shipment.getRegisteredBy().getFirstName() + " " + shipment.getRegisteredBy().getLastName();

        return ShipmentViewDto.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .type(shipment.getType())
                .status(shipment.getStatus())
                .weight(shipment.getWeight())
                .length(shipment.getLength())
                .width(shipment.getWidth())
                .height(shipment.getHeight())
                .totalPrice(shipment.getTotalPrice())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .senderId(shipment.getSender().getId())
                .senderName(senderName.trim())
                .senderPhone(shipment.getSender().getPhoneNumber())
                .receiverId(shipment.getReceiver().getId())
                .receiverName(receiverName.trim())
                .receiverPhone(shipment.getReceiver().getPhoneNumber())
                .deliveryOfficeId(deliveryOfficeId)
                .deliveryOfficeName(deliveryOfficeName)
                .deliveryAddressString(deliveryAddressString)
                .registeredById(shipment.getRegisteredBy().getId())
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
