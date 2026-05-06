package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.ShipmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new shipment.
 * Supports sending to either a registered user (receiverId) OR a guest (receiverName + receiverPhone).
 * Requires either a deliveryOfficeId OR a deliveryAddress, but not both.
 */
@Builder
public record ShipmentCreationDto(
        @NotNull(message = "{validation.shipment.sender.notnull}")
        UUID senderId,

        // Receiver can be a registered client OR a guest. 
        // Validation is handled at the Service layer (XOR logic).
        UUID receiverId,
        String receiverName,
        String receiverPhone,
        String receiverEmail,

        @NotNull(message = "{validation.shipment.type.notnull}")
        ShipmentType type,

        @NotNull(message = "{validation.shipment.weight.notnull}")
        @Positive(message = "{validation.shipment.weight.positive}")
        BigDecimal weight,

        // Optional dimensions
        @Positive(message = "{validation.shipment.dimension.positive}")
        BigDecimal length,
        
        @Positive(message = "{validation.shipment.dimension.positive}")
        BigDecimal width,
        
        @Positive(message = "{validation.shipment.dimension.positive}")
        BigDecimal height,

        // Destination details (Mutually Exclusive in business logic)
        Long deliveryOfficeId,
        @Valid AddressDetailsDto deliveryAddress
) {
}
