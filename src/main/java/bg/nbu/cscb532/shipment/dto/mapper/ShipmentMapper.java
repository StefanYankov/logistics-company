package bg.nbu.cscb532.shipment.dto.mapper;

import bg.nbu.cscb532.shipment.Shipment;
import bg.nbu.cscb532.shipment.dto.PublicShipmentViewDto;
import bg.nbu.cscb532.shipment.dto.StaffShipmentViewDto;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Component responsible for mapping the {@link Shipment} entity to its various Data Transfer Objects (DTOs).
 * This centralizes the transformation logic, separating it from the business logic in the service layer.
 */
@Component
public class ShipmentMapper {

    /**
     * Maps a {@link Shipment} entity to a {@link StaffShipmentViewDto}.
     * This DTO contains detailed, sensitive information intended for internal staff or authorized clients.
     * It correctly prioritizes denormalized "snapshot" fields for receiver details over the linked client's profile data.
     *
     * @param shipment The Shipment entity to map.
     * @return A fully populated StaffShipmentViewDto.
     */
    public StaffShipmentViewDto toStaffView(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        // --- Location Details ---
        Long originOfficeId = (shipment.getOriginOffice() != null) ? shipment.getOriginOffice().getId() : null;
        String originOfficeName = (shipment.getOriginOffice() != null) ? formatOfficeName(shipment.getOriginOffice()) : null;
        String originAddressString = (shipment.getOriginAddressSnapshot() != null) ? formatAddress(shipment.getOriginAddressSnapshot()) : null;

        Long deliveryOfficeId = (shipment.getDeliveryOffice() != null) ? shipment.getDeliveryOffice().getId() : null;
        String deliveryOfficeName = (shipment.getDeliveryOffice() != null) ? formatOfficeName(shipment.getDeliveryOffice()) : null;
        String deliveryAddressString = (shipment.getDeliveryAddressSnapshot() != null) ? formatAddress(shipment.getDeliveryAddressSnapshot()) : null;

        // --- Receiver Details (Snapshot Priority Logic) ---
        // The denormalized fields on the Shipment entity are the "source of truth" for the DTO.
        // This ensures that any edits (e.g., correcting a typo) are always reflected.
        String receiverName = shipment.getReceiverName();
        String receiverPhone = shipment.getReceiverPhone();
        UUID receiverId = (shipment.getReceiver() != null) ? shipment.getReceiver().getId() : null;

        // Fallback: If the denormalized fields are empty, use the data from the linked Client profile.
        if ((receiverName == null || receiverName.isBlank()) && shipment.getReceiver() != null) {
            receiverName = shipment.getReceiver().getFirstName() + " " + shipment.getReceiver().getLastName();
        }
        if ((receiverPhone == null || receiverPhone.isBlank()) && shipment.getReceiver() != null) {
            receiverPhone = shipment.getReceiver().getPhoneNumber();
        }

        String currentOfficeName = (shipment.getCurrentOffice() != null) ? formatOfficeName(shipment.getCurrentOffice()) : null;
        String currentCourierName = (shipment.getCurrentCourier() != null) ? shipment.getCurrentCourier().getFirstName() + " " + shipment.getCurrentCourier().getLastName() : null;

        return StaffShipmentViewDto.builder()
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
                .senderName(shipment.getSender().getFirstName() + " " + shipment.getSender().getLastName())
                .senderPhone(shipment.getSender().getPhoneNumber())
                .receiverId(receiverId)
                .receiverName(receiverName != null ? receiverName.trim() : null)
                .receiverPhone(receiverPhone)
                .originOfficeId(originOfficeId)
                .originOfficeName(originOfficeName)
                .originAddressString(originAddressString)
                .deliveryOfficeId(deliveryOfficeId)
                .deliveryOfficeName(deliveryOfficeName)
                .deliveryAddressString(deliveryAddressString)
                .currentOfficeId(shipment.getCurrentOffice() != null ? shipment.getCurrentOffice().getId() : null)
                .currentOfficeName(currentOfficeName)
                .currentCourierId(shipment.getCurrentCourier() != null ? shipment.getCurrentCourier().getId() : null)
                .currentCourierName(currentCourierName)
                .registeredById(shipment.getRegisteredBy() != null ? shipment.getRegisteredBy().getId() : null)
                .registeredByName(shipment.getRegisteredBy() != null ? shipment.getRegisteredBy().getFirstName() + " " + shipment.getRegisteredBy().getLastName() : "Self-Registered")
                .appliedAddons(shipment.getAddons().stream().map(a -> a.getServiceCatalog().getName()).collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps a {@link Shipment} entity to a {@link PublicShipmentViewDto}.
     * This DTO contains a restricted, publicly safe subset of shipment data suitable for anonymous tracking.
     *
     * @param shipment The Shipment entity to map.
     * @return A populated PublicShipmentViewDto.
     */
    public PublicShipmentViewDto toPublicView(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        String originCityName = (shipment.getOriginOffice() != null)
                ? shipment.getOriginOffice().getAddressDetails().getCity().getName()
                : (shipment.getOriginAddressSnapshot() != null ? shipment.getOriginAddressSnapshot().getCity().getName() : null);

        String deliveryCityName = (shipment.getDeliveryOffice() != null)
                ? shipment.getDeliveryOffice().getAddressDetails().getCity().getName()
                : (shipment.getDeliveryAddressSnapshot() != null ? shipment.getDeliveryAddressSnapshot().getCity().getName() : null);

        return PublicShipmentViewDto.builder()
                .trackingNumber(shipment.getTrackingNumber())
                .type(shipment.getPackageDetails().getType())
                .status(shipment.getStatus())
                .weight(shipment.getPackageDetails().getWeight())
                .length(shipment.getPackageDetails().getLength())
                .width(shipment.getPackageDetails().getWidth())
                .height(shipment.getPackageDetails().getHeight())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .originCityName(originCityName)
                .destinationCityName(deliveryCityName)
                .currentOfficeName((shipment.getCurrentOffice() != null) ? formatOfficeName(shipment.getCurrentOffice()) : null)
                .appliedAddons(shipment.getAddons().stream().map(a -> a.getServiceCatalog().getName()).collect(Collectors.toList()))
                .build();
    }

    // --- Private Helper Methods ---

    private String formatAddress(bg.nbu.cscb532.shared.location.AddressDetails address) {
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

    private String formatOfficeName(bg.nbu.cscb532.office.Office office) {
        if (office == null || office.getAddressDetails() == null) {
            return "";
        }
        return office.getAddressDetails().getCity().getName() + " - " + office.getAddressDetails().getStreet();
    }

    private String formatOptionalPart(String prefix, String value) {
        if (value != null && !value.isBlank()) {
            return prefix + value;
        }
        return null;
    }
}