package bg.nbu.cscb532.shipment.dto;

import bg.nbu.cscb532.shipment.PaidBy;
import bg.nbu.cscb532.shipment.ShipmentStatus;
import bg.nbu.cscb532.shipment.ShipmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the public view of a Shipment.
 * Flattens the complex entity graph (Clients, Offices) into a simple, secure JSON structure.
 */
@Builder
public record ShipmentViewDto(
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
        String registeredByName
) {
}
