package com.example.project.customer.service;

import com.example.project.customer.dto.ResendEmailRequest;
import com.example.project.customer.dto.ResendEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ResendEmailServiceImpl implements EmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final String replyToEmail;

    public ResendEmailServiceImpl(
            @Value("${resend.api.url:https://api.resend.com/emails}") String apiUrl,
            @Value("${resend.api.key:}") String apiKey,
            @Value("${resend.from.email:noreply@hinchmart.com}") String fromEmail,
            @Value("${resend.from.name:HinchMart}") String fromName,
            @Value("${resend.reply-to.email:hinchmart@gmail.com}") String replyToEmail) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "noreply@hinchmart.com";
        this.fromName = fromName != null ? fromName.trim() : "HinchMart";
        this.replyToEmail = replyToEmail != null ? replyToEmail.trim() : "hinchmart@gmail.com";

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendLoginSuccessEmail(String toEmail, String userName, String ipAddress, String userAgent) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send login notification email: Recipient email is null or blank.");
            return;
        }

        if (apiKey.isEmpty() || apiKey.startsWith("re_placeholder")) {
            log.warn("Resend API key is not configured or using placeholder. Skipping email delivery to: {}", toEmail);
            return;
        }

        try {
            String formattedTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, hh:mm a 'IST'"));

            String displayName = (userName != null && !userName.trim().isEmpty()) ? userName.trim() : "Valued Customer";
            String clientIp = (ipAddress != null && !ipAddress.trim().isEmpty()) ? ipAddress.trim() : "Not detected";
            String clientDevice = formatUserAgent(userAgent);

            String subject = "Security Alert: Successful Login to Your HinchMart Account";
            String htmlBody = buildLoginHtmlTemplate(displayName, toEmail, formattedTime, clientIp, clientDevice);
            String textBody = buildLoginTextTemplate(displayName, formattedTime, clientIp, clientDevice);

            String sender = String.format("%s <%s>", fromName, fromEmail);

            ResendEmailRequest request = ResendEmailRequest.builder()
                    .from(sender)
                    .to(List.of(toEmail.trim()))
                    .subject(subject)
                    .html(htmlBody)
                    .text(textBody)
                    .replyTo(replyToEmail)
                    .build();

            ResendEmailResponse response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ResendEmailResponse.class);

            log.info("Login alert email sent successfully to {} via Resend. Resend Email ID: {}",
                    toEmail, response != null ? response.getId() : "unknown");

        } catch (Exception ex) {
            log.error("Failed to send login alert email via Resend to {}. Error: {}", toEmail, ex.getMessage(), ex);
        }
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send email: Recipient is null or blank.");
            return;
        }

        if (apiKey.isEmpty() || apiKey.startsWith("re_placeholder")) {
            log.warn("Resend API key is not configured. Skipping email delivery to: {}", toEmail);
            return;
        }

        try {
            String sender = String.format("%s <%s>", fromName, fromEmail);

            ResendEmailRequest request = ResendEmailRequest.builder()
                    .from(sender)
                    .to(List.of(toEmail.trim()))
                    .subject(subject)
                    .html(htmlContent)
                    .replyTo(replyToEmail)
                    .build();

            ResendEmailResponse response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ResendEmailResponse.class);

            log.info("Email '{}' sent successfully to {} via Resend. ID: {}",
                    subject, toEmail, response != null ? response.getId() : "unknown");

        } catch (Exception ex) {
            log.error("Failed to send email via Resend to {}. Error: {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildLoginHtmlTemplate(String userName, String email, String time, String ip, String device) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; color: #1e293b;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 40px 15px;">
                    <tr>
                        <td align="center">
                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 580px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1);">
                                
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #1e3a8a 0%%, #2563eb 100%%); padding: 28px 32px; text-align: left;">
                                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">HinchMart</h1>
                                        <p style="margin: 6px 0 0 0; font-size: 13px; color: #bfdbfe;">Account Security Notification</p>
                                    </td>
                                </tr>

                                <!-- Content Body -->
                                <tr>
                                    <td style="padding: 32px;">
                                        <h2 style="margin: 0 0 16px 0; font-size: 18px; font-weight: 600; color: #0f172a;">Successful Login Alert</h2>
                                        <p style="margin: 0 0 20px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                                            Hello <strong style="color: #0f172a;">%s</strong>,
                                        </p>
                                        <p style="margin: 0 0 24px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                                            We noticed a successful login to your HinchMart account (<strong>%s</strong>). Here are the details of the session:
                                        </p>

                                        <!-- Details Card -->
                                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: 24px;">
                                            <tr>
                                                <td style="padding: 16px 20px;">
                                                    <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                                        <tr>
                                                            <td width="35%%" style="padding: 6px 0; font-size: 13px; color: #64748b; font-weight: 500;">Login Time:</td>
                                                            <td style="padding: 6px 0; font-size: 13px; color: #0f172a; font-weight: 600;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 6px 0; font-size: 13px; color: #64748b; font-weight: 500;">IP Address:</td>
                                                            <td style="padding: 6px 0; font-size: 13px; color: #0f172a; font-weight: 600;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 6px 0; font-size: 13px; color: #64748b; font-weight: 500;">Device / Client:</td>
                                                            <td style="padding: 6px 0; font-size: 13px; color: #0f172a; font-weight: 600;">%s</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Warning Notice -->
                                        <div style="background-color: #fff7ed; border-left: 4px solid #f97316; padding: 14px 16px; border-radius: 4px; margin-bottom: 24px;">
                                            <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #9a3412;">
                                                <strong>Didn't log in?</strong> If you did not perform this action, someone else may have accessed your account. Please reset your password immediately and notify us at <a href="mailto:hinchmart@gmail.com" style="color: #c2410c; text-decoration: underline;">hinchmart@gmail.com</a>.
                                            </p>
                                        </div>

                                        <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #64748b;">
                                            Best regards,<br>
                                            <strong>The HinchMart Security Team</strong>
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 32px; text-align: center;">
                                        <p style="margin: 0 0 6px 0; font-size: 12px; color: #94a3b8;">
                                            This is an automated security notification. For support, reply to this email or reach out to <a href="mailto:hinchmart@gmail.com" style="color: #2563eb; text-decoration: none;">hinchmart@gmail.com</a>.
                                        </p>
                                        <p style="margin: 0; font-size: 11px; color: #cbd5e1;">
                                            &copy; 2026 HinchMart Inc. All rights reserved.
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName, email, time, ip, device);
    }

    private String buildLoginTextTemplate(String userName, String time, String ip, String device) {
        return """
            Hello %s,

            We noticed a successful login to your HinchMart account.

            Session Details:
            - Time: %s
            - IP Address: %s
            - Device / Client: %s

            If you did not perform this login, please reset your password immediately or contact our support team at hinchmart@gmail.com.

            Best regards,
            The HinchMart Security Team
            """.formatted(userName, time, ip, device);
    }

    private String formatUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "Standard Browser / Web Client";
        }
        String ua = userAgent.trim();
        if (ua.contains("Postman")) return "Postman API Client";
        if (ua.contains("Edg/")) return "Microsoft Edge";
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Google Chrome";
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Apple Safari";
        if (ua.contains("Firefox/")) return "Mozilla Firefox";
        if (ua.length() > 60) {
            return ua.substring(0, 60) + "...";
        }
        return ua;
    }
}
