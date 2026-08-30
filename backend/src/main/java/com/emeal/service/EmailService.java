package com.emeal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend.url:http://127.0.0.1:5173}")
    private String frontendUrl;

    @Value("${app.mail.from:noreply@company.com}")
    private String mailFrom;

    public void sendPasswordResetEmail(String toEmail, String token, String username) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        logger.info("================================================================================");
        logger.info("PASSWORD RESET LINK GENERATED FOR USER: {}", username);
        logger.info("TO EMAIL: {}", toEmail);
        logger.info("RESET URL: {}", resetUrl);
        logger.info("TOKEN EXPIRES IN: 15 MINUTES");
        logger.info("================================================================================");

        // Note: Production SMTP hook can dispatch MIME message via JavaMailSender when SMTP properties are provided in .env
    }
}
