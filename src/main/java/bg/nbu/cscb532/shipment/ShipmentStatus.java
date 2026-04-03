package bg.nbu.cscb532.shipment;

/**
 * Represents the current lifecycle state of a shipment.
 */
public enum ShipmentStatus {
    REGISTERED,
    IN_TRANSIT,
    AT_DELIVERY_OFFICE,
    OUT_FOR_DELIVERY,
    DELIVERED
}
