package com.vulprioritizer.backend_part.auth.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * No-op email service — SMTP dependency has been removed.
 *
 * <p>In development, verification/reset links are printed to the log so they
 * can still be copied from the console without a real mail server.</p>
 *
 * <p>To re-enable real email delivery: add spring-boot-starter-mail back to
 * pom.xml, restore the JavaMailSender injection, and add the mail.*
 * properties to application.yaml.</p>
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Logs the email content instead of sending it.
     * Replace this body with real SMTP sending when a mail server is available.
     */
    public void sendMail(String to, String subject, String body) {
        log.info("=== [EMAIL STUB] ===");
        log.info("  To      : {}", to);
        log.info("  Subject : {}", subject);
        log.info("  Body    : {}", body);
        log.info("===================");
    }
}
