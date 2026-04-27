package bg.nbu.cscb532.shipment.dto;

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
        BigDecimal totalPrice,
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

        // Destination summary (Mutually Exclusive based on Pricing Engine rules)
        Long deliveryOfficeId,
        String deliveryOfficeName,
        String deliveryAddressString, // Formatted flat string if delivered to a home address

        // Employee summary
        UUID registeredById,
        String registeredByName
) {
}
