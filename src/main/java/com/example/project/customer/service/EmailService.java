package com.example.project.customer.service;

public interface EmailService {

    /**
     * Sends a login notification email to the user asynchronously.
     *
     * @param toEmail   Recipient's email address
     * @param userName  Recipient's full name
     * @param ipAddress IP address from which the login occurred
     * @param userAgent Browser/device user-agent string
     */
    void sendLoginSuccessEmail(String toEmail, String userName, String ipAddress, String userAgent);

    /**
     * Generic method to send an HTML email asynchronously.
     *
     * @param toEmail     Recipient's email address
     * @param subject     Email subject
     * @param htmlContent HTML body content
     */
    void sendEmail(String toEmail, String subject, String htmlContent);
}
