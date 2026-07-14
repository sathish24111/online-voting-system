package com.college.voting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.username.secondary:}")
    private String mailUsernameSecondary;

    @Value("${spring.mail.password.secondary:}")
    private String mailPasswordSecondary;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private JavaMailSenderImpl createSecondaryMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(465);
        sender.setUsername(mailUsernameSecondary);
        sender.setPassword(mailPasswordSecondary);
        
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        return sender;
    }

    @Async
    public void sendOtpEmail(String recipientEmail, String studentName, String otp) {
        String subject = "Department Election Verification OTP";
        String body = String.format(
            "Hello %s,\n\n" +
            "Your 6-digit verification code for the Department Online Voting System is: %s\n\n" +
            "This OTP is valid for 5 minutes. Do not share this code with anyone.\n\n" +
            "Regards,\n" +
            "Election Commission",
            studentName, otp
        );

        if ("your-email@gmail.com".equalsIgnoreCase(mailUsername)) {
            logOtpConsoleFallback(recipientEmail, studentName, otp, "Default SMTP credentials configured");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Successfully sent OTP email via primary SMTP to {}", recipientEmail);
        } catch (Exception e) {
            logger.warn("Primary SMTP failed: {}. Attempting secondary fallback...", e.getMessage());
            
            if (mailUsernameSecondary != null && !mailUsernameSecondary.trim().isEmpty() 
                && !"your-secondary-email@gmail.com".equalsIgnoreCase(mailUsernameSecondary.trim())) {
                try {
                    JavaMailSenderImpl secondarySender = createSecondaryMailSender();
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailUsernameSecondary);
                    message.setTo(recipientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    secondarySender.send(message);
                    logger.info("Successfully sent OTP email via secondary SMTP to {}", recipientEmail);
                    return;
                } catch (Exception ex) {
                    logger.warn("Secondary SMTP failover also failed: {}", ex.getMessage());
                }
            }
            
            logger.warn("All SMTP options failed, falling back to console log print.");
            logOtpConsoleFallback(recipientEmail, studentName, otp, e.getMessage());
        }
    }

    private void logOtpConsoleFallback(String email, String name, String otp, String reason) {
        System.out.println("\n");
        System.out.println("==================================================================");
        System.out.println("                   [OTP EMAIL FALLBACK LOG]                       ");
        System.out.println("==================================================================");
        System.out.println("Recipient Name  : " + name);
        System.out.println("Recipient Email : " + email);
        System.out.println("Generated OTP   : " + otp);
        System.out.println("Reason          : " + reason);
        System.out.println("==================================================================");
        System.out.println("\n");
    }
}
