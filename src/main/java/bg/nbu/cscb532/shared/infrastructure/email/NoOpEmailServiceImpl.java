package bg.nbu.cscb532.shared.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * A dummy implementation of the EmailService for local development.
 * Instead of physically calling a paid API (like SendGrid), this safely dumps
 * the links and tokens to the application console.
 * This satisfies the architectural boundary while completely decoupling the tests
 * from internet reliability.
 */
@Slf4j
@Service
public class NoOpEmailServiceImpl implements EmailService {

    // Usually, this base URL would be injected from application.yaml via @Value("${app.frontend.url}")
    private static final String MOCK_FRONTEND_URL = "http://localhost:4200";

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationLink = MOCK_FRONTEND_URL + "/verify?token=" + token;
        
        log.info("==========================================================================");
        log.info("📧 MOCK EMAIL DISPATCHED");
        log.info("Type: EMAIL VERIFICATION");
        log.info("To: {}", to);
        log.info("Action Required: Please click the following link to activate your account.");
        log.info("Link: {}", verificationLink);
        log.info("==========================================================================");
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = MOCK_FRONTEND_URL + "/reset-password?token=" + token;
        
        log.info("==========================================================================");
        log.info("📧 MOCK EMAIL DISPATCHED");
        log.info("Type: PASSWORD RESET");
        log.info("To: {}", to);
        log.info("Action Required: Please click the following link to reset your password.");
        log.info("Link: {}", resetLink);
        log.info("==========================================================================");
    }
}
