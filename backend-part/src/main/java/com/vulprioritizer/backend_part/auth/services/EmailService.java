package com.vulprioritizer.backend_part.auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * Sends an email, tolerating SMTP failures: auth flows (registration, password
     * reset) must not die with a 500 just because the mail server is unreachable
     * (offline, DNS down, port 587 blocked on the network).
     *
     * <p>On failure the error is logged together with the message body so that in local
     * development the verification/reset link can still be copied from the console.
     * Callers already persist their token before calling this, so nothing is lost.</p>
     */
    public void sendMail(String to, String subject, String body){
        SimpleMailMessage mailMessage=new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setFrom(from);
        mailMessage.setText(body);

        try{
            mailSender.send(mailMessage);
            log.debug("Email sent to {} (subject: {})", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email to {} (subject: {}): {}",
                    to, subject, e.getMessage());
            log.warn("Email content (dev helper) -> to: {} | {} | {}",
                    to, subject, body);
        }
    }
}
