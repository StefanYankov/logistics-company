package bg.nbu.cscb532.shared.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for cryptographic operations.
 * Centralizes hashing logic to ensure consistency across Client and Employee domains.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {

    /**
     * Hashes a raw string using SHA-256 and returns the hex representation.
     * Used for storing verification and reset tokens securely.
     *
     * @param input The raw token string.
     * @return A 64-character hex string.
     * @throws RuntimeException if the SHA-256 algorithm is unavailable.
     */
    public static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // This should never happen in a standard JVM, but we wrap it as a
            // runtime exception to avoid polluting business logic with checked exceptions.
            throw new IllegalStateException("Critical Error: SHA-256 algorithm not found", e);
        }
    }
}