package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shipment.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Data Transfer Object for updating the status of a shipment.
 */
@Builder
public record ShipmentStatusUpdateDto(

        @NotNull(message = "{validation.shipment.status.notnull}")
        ShipmentStatus newStatus,
        Long locationOfficeId,
        String notes
) {
}
