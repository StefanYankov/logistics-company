package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shared.Constants;
import bg.nbu.cscb532.shared.location.AddressDetailsDto;
import bg.nbu.cscb532.shipment.PaidBy;
import bg.nbu.cscb532.shipment.ShipmentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Data Transfer Object for updating an existing shipment.
 * This DTO omits immutable fields like trackingNumber, senderId, and origin details.
 */
@Builder
public record ShipmentUpdateDto(
        ShipmentType type,

        @DecimalMin(value = Constants.Validation.MIN_WEIGHT, message = "{validation.shipment.weight.min}")
        @DecimalMax(value = Constants.Validation.MAX_WEIGHT, message = "{validation.shipment.weight.max}")
        BigDecimal weight,

        BigDecimal length,
        BigDecimal width,
        BigDecimal height,

        PaidBy paidBy,
        Boolean isPaid,

        String receiverName,

        @Pattern(regexp = Constants.Validation.PHONE_REGEX, message = "{validation.user.phone.format}")
        String receiverPhone,

        @Email(message = "{validation.user.email.format}")
        String receiverEmail,

        Set<Long> selectedServiceIds,

        Long deliveryOfficeId,
        AddressDetailsDto deliveryAddress
) {}
