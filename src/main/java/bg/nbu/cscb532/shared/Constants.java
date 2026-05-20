package bg.nbu.cscb532.shared;

/**
 * Global application constants to eliminate magic numbers and strings.
 * Used across Entities for database schema generation and DTOs for validation.
 */
public final class Constants {

    private Constants() {
        // Restrict instantiation
    }

    public static final class Validation {
        // Names (Cities, Offices, Users, etc.)
        public static final int MIN_NAME_LENGTH = 2;
        public static final int MAX_NAME_LENGTH = 100;

        // Postcode
        public static final int MIN_POSTCODE_LENGTH = 4;
        public static final int MAX_POSTCODE_LENGTH = 5;
        
        // Addresses
        public static final int MAX_STREET_LENGTH = 100;
        public static final int MAX_DISTRICT_LENGTH = 100;
        public static final int MAX_BUILDING_INFO_LENGTH = 10;
        
        // Identifiers
        public static final int MAX_REGISTRATION_NUMBER_LENGTH = 50;
        public static final int MAX_PHONE_NUMBER_LENGTH = 20;
        public static final int MAX_POSTAL_CODE_LENGTH = 5;
        
        // Shipments
        public static final String MIN_WEIGHT = "0.1";
        public static final String MAX_WEIGHT = "1000.0";
        
        // Security
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_PASSWORD_LENGTH = 128;
        public static final int MAX_PHONE_LENGTH = 16;
        public static final int MAX_EMAIL_LENGTH = 255;
        
        // Regex
        public static final String PHONE_REGEX = "^\\+?[0-9]{8,15}$";
    }

    /**
     * Standardized internal error messages for developer logging and defense-in-depth assertions.
     */
    public static final class DeveloperErrors {
        public static final String DTO_NULL = "Incoming DTO must not be null.";
        public static final String ENTITY_ID_NULL = "Entity ID must not be null.";
        public static final String PAGEABLE_NULL = "Pageable parameter must not be null.";
        public static final String NAME_NULL = "Name must not be null.";
    }
}
