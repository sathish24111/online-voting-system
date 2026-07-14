package com.college.voting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        String htmlContent = buildHtmlOtpTemplate(otp);

        if ("your-email@gmail.com".equalsIgnoreCase(mailUsername)) {
            logOtpConsoleFallback(recipientEmail, studentName, otp, "Default SMTP credentials configured");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            helper.setFrom(mailUsername);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            logger.info("Successfully sent OTP email via primary SMTP to {}", recipientEmail);
        } catch (Exception e) {
            logger.warn("Primary SMTP failed: {}. Attempting secondary fallback...", e.getMessage());
            
            if (mailUsernameSecondary != null && !mailUsernameSecondary.trim().isEmpty() 
                && !"your-secondary-email@gmail.com".equalsIgnoreCase(mailUsernameSecondary.trim())) {
                try {
                    JavaMailSenderImpl secondarySender = createSecondaryMailSender();
                    MimeMessage mimeMessage = secondarySender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
                    helper.setFrom(mailUsernameSecondary);
                    helper.setTo(recipientEmail);
                    helper.setSubject(subject);
                    helper.setText(htmlContent, true);
                    
                    secondarySender.send(mimeMessage);
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

    private String buildHtmlOtpTemplate(String otp) {
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Verification Code</title>\n" +
            "</head>\n" +
            "<body style=\"margin: 0; padding: 0; background-color: #F5F7FA; font-family: Arial, Helvetica, sans-serif; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%;\">\n" +
            "    <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #F5F7FA; padding: 40px 10px;\">\n" +
            "        <tr>\n" +
            "            <td align=\"center\">\n" +
            "                <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #FFFFFF; border: 1px solid #E5E7EB; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); overflow: hidden;\">\n" +
            "                    <!-- Header -->\n" +
            "                    <tr>\n" +
            "                        <td align=\"center\" style=\"padding: 40px 40px 20px 40px;\">\n" +
            "                            <h2 style=\"margin: 0; font-family: Arial, Helvetica, sans-serif; font-size: 24px; font-weight: bold; color: #2563EB; text-align: center; line-height: 1.3;\">\n" +
            "                                College Online Voting System\n" +
            "                            </h2>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                    <!-- Divider -->\n" +
            "                    <tr>\n" +
            "                        <td style=\"padding: 0 40px;\">\n" +
            "                            <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-top: 1px solid #E5E7EB;\">\n" +
            "                                <tr><td></td></tr>\n" +
            "                            </table>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                    <!-- Content Body -->\n" +
            "                    <tr>\n" +
            "                        <td style=\"padding: 30px 40px 20px 40px; font-family: Arial, Helvetica, sans-serif; font-size: 16px; color: #374151; line-height: 1.6; text-align: left;\">\n" +
            "                            <p style=\"margin: 0 0 16px 0;\">Hello,</p>\n" +
            "                            <p style=\"margin: 0 0 30px 0;\">You have requested a secure verification code to sign into the Online Voting System.</p>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                    <!-- OTP Code Box -->\n" +
            "                    <tr>\n" +
            "                        <td align=\"center\" style=\"padding: 0 40px 30px 40px;\">\n" +
            "                            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #F3F4F6; border-radius: 8px; width: 100%;\">\n" +
            "                                <tr>\n" +
            "                                    <td align=\"center\" style=\"padding: 24px; font-family: Arial, Helvetica, sans-serif; font-size: 42px; font-weight: bold; color: #1E3A8A; letter-spacing: 10px; text-align: center;\">\n" +
            "                                        %s\n" +
            "                                    </td>\n" +
            "                                </tr>\n" +
            "                            </table>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                    <!-- Expiry Msg -->\n" +
            "                    <tr>\n" +
            "                        <td align=\"center\" style=\"padding: 0 40px 40px 40px; font-family: Arial, Helvetica, sans-serif; font-size: 14px; color: #6B7280; line-height: 1.5; text-align: center;\">\n" +
            "                            <p style=\"margin: 0 0 4px 0;\">This code is valid for 5 minutes.</p>\n" +
            "                            <p style=\"margin: 0;\">Do not share this OTP with anyone.</p>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                    <!-- Footer Divider -->\n" +
            "                    <tr>\n" +
            "                        <td style=\"padding: 0 40px;\">\n" +
            "                            <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-top: 1px solid #E5E7EB;\">\n" +
            "                                <tr><td></td></tr>\n" +
            "                            </table>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                    <!-- Footer -->\n" +
            "                    <tr>\n" +
            "                        <td align=\"center\" style=\"padding: 30px 40px; font-family: Arial, Helvetica, sans-serif; font-size: 12px; color: #9CA3AF; text-align: center; line-height: 1.5;\">\n" +
            "                            <p style=\"margin: 0 0 4px 0;\">This is an automated system email.</p>\n" +
            "                            <p style=\"margin: 0;\">Please do not reply directly.</p>\n" +
            "                        </td>\n" +
            "                    </tr>\n" +
            "                </table>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "    </table>\n" +
            "</body>\n" +
            "</html>",
            otp
        );
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
