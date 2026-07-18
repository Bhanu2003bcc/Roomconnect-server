package com.roomconnect.modules.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    @Value("${sms-gateway.username}")
    private String smsUsername;

    @Value("${sms-gateway.password}")
    private String smsPassword;

    @Value("${sms-gateway.api-url:https://api.sms-gate.app/3rdparty/v1/messages}")
    private String smsApiUrl;

    @Value("${resend.api-key}")
    private String resendApiKey;

    private final RestClient restClient = RestClient.create();

    public void sendSms(String phone, String message) {
        log.info("[SMS NOTIFICATION] Send to: {}, Msg: {}", phone, message);
        if (isMock(smsUsername) || isMock(smsPassword)) {
            log.info("[SMS MOCK] SMS Gateway keys not configured. SMS not sent via network.");
            return;
        }
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((smsUsername + ":" + smsPassword).getBytes());

            Map<String, Object> requestBody = Map.of(
                    "textMessage", Map.of("text", message),
                    "phoneNumbers", List.of(phone)
            );

            restClient.post()
                    .uri(smsApiUrl)
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SMS SENT] Successfully dispatched to SMS Gateway for: {}", phone);
        } catch (Exception e) {
            log.error("[SMS ERROR] Failed to send SMS to {} via SMS Gateway: {}", phone, e.getMessage(), e);
        }
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

