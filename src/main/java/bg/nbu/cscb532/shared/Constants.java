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
        public static final int MAX_NAME_LENGTH = 100;
        
        // Addresses
        public static final int MAX_STREET_LENGTH = 100;
        public static final int MAX_DISTRICT_LENGTH = 100;
        public static final int MAX_BUILDING_INFO_LENGTH = 10;
        
        // Identifiers
        public static final int MAX_REGISTRATION_NUMBER_LENGTH = 50;
        public static final int MAX_PHONE_NUMBER_LENGTH = 20;
        public static final int MAX_POSTAL_CODE_LENGTH = 5;
        
        // Security
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_PASSWORD_LENGTH = 128;
    }
}
