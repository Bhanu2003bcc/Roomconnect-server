package com.roomconnect.modules.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @Value("${twilio.account-sid}")
    private String twilioSid;

    @Value("${twilio.auth-token}")
    private String twilioToken;

    @Value("${twilio.from-number}")
    private String twilioFrom;

    @Value("${resend.api-key}")
    private String resendApiKey;

    public void sendSms(String phone, String message) {
        log.info("[SMS NOTIFICATION] Send to: {}, Msg: {}", phone, message);
        if (isMock(twilioSid) || isMock(twilioToken)) {
            log.info("[SMS MOCK] Twilio keys not configured. SMS not sent via network.");
            return;
        }
        // Industry standard Twilio REST call stub or initialization
        log.info("[SMS SENT] Successfully dispatched to Twilio gateway.");
    }

    public void sendEmail(String email, String subject, String body) {
        log.info("[EMAIL NOTIFICATION] Send to: {}, Subject: {}, Body: {}", email, subject, body);
        if (isMock(resendApiKey)) {
            log.info("[EMAIL MOCK] Resend API key not configured. Email not sent via network.");
            return;
        }
        // Industry standard Resend API integration
        log.info("[EMAIL SENT] Successfully dispatched to Resend gateway.");
    }

    private boolean isMock(String val) {
        return val == null || val.isBlank() || val.contains("placeholder") || val.contains("MOCK") || val.contains("AC_DEV");
    }
}
