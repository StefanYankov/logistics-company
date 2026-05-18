package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shipment.PaidBy;
import bg.nbu.cscb532.shipment.ShipmentStatus;
import bg.nbu.cscb532.shipment.ShipmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A comprehensive, internal-facing view of a shipment for staff members.
 * Contains the full, uncensored data including PII and financial details.
 */
@Builder
public record StaffShipmentViewDto(
        UUID id,
        String trackingNumber,
        ShipmentType type,
        ShipmentStatus status,
        BigDecimal weight,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,

        // Financials
        BigDecimal totalPrice,
        PaidBy paidBy,
        boolean isPaid,

        Instant createdAt,
        Instant updatedAt,

        // Sender summary
        UUID senderId,
        String senderName,
        String senderPhone,

        // Receiver summary
        UUID receiverId,
        String receiverName,
        String receiverPhone,

        // Origin summary
        Long originOfficeId,
        String originOfficeName,
        String originAddressString,

        // Destination summary
        Long deliveryOfficeId,
        String deliveryOfficeName,
        String deliveryAddressString,

        // Current Location summary
        Long currentOfficeId,
        String currentOfficeName,
        UUID currentCourierId,
        String currentCourierName,

        // Employee summary
        UUID registeredById,
        String registeredByName,

        // Addons
        List<String> appliedAddons
) {
}
