package bg.nbu.cscb532.shipment;

/**
 * Defines the physical category of the shipment, which dictates validation rules (e.g., documents don't need dimensions).
 */
public enum ShipmentType {
    DOCUMENT,
    PARCEL,
    PALLET,
    OVERSIZED
}
