package com.vulprioritizer.backend_part.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.base-url")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String from;

    public void sendMail(String to, String subject, String body){
        SimpleMailMessage mailMessage=new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setFrom(from);
        mailMessage.setText(body);

        try{
            mailSender.send(mailMessage);

        } catch (MailException e) {
            throw new RuntimeException(e);
        }
    }




}
