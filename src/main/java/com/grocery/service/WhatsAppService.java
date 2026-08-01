package com.grocery.service;

import com.grocery.dto.WhatsAppSendResponse;
import com.grocery.model.GroceryItem;
import com.grocery.model.SentMessage;
import com.grocery.repository.SentMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SentMessageRepository sentMessageRepository;

    public WhatsAppService(SentMessageRepository sentMessageRepository) {
        this.sentMessageRepository = sentMessageRepository;
    }

    // automatic=false for a manual tap of "Send List", true when the scheduler fired it.
    public WhatsAppSendResponse sendGroceryList(String username, String toNumber, LocalDate date,
                                                 List<GroceryItem> items, boolean automatic) {
        String text = buildMessage(date, items);
        String digitsOnlyNumber = toNumber.replaceAll("[^0-9]", "");

        boolean apiConfigured = phoneNumberId != null && !phoneNumberId.isBlank()
                && accessToken != null && !accessToken.isBlank();

        WhatsAppSendResponse response;
        if (apiConfigured) {
            try {
                sendViaCloudApi(digitsOnlyNumber, text);
                response = new WhatsAppSendResponse(true, "Message sent via WhatsApp Cloud API.", null);
            } catch (Exception ex) {
                // Fall back to a wa.me link if the direct send fails (e.g. bad token, unverified number)
                String link = buildWaLink(digitsOnlyNumber, text);
                response = new WhatsAppSendResponse(false,
                        "Direct send failed (" + ex.getMessage() + "). Use the link instead.", link);
            }
        } else {
            String link = buildWaLink(digitsOnlyNumber, text);
            response = new WhatsAppSendResponse(false,
                    "WhatsApp Cloud API not configured on the server - open this link to send manually.", link);
        }

        sentMessageRepository.save(new SentMessage(username, date, digitsOnlyNumber, "WHATSAPP", text,
                response.isSentDirectly(), automatic));
        return response;
    }

    private void sendViaCloudApi(String toNumber, String text) {
        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toNumber);
        body.put("type", "text");
        body.put("text", Map.of("body", text));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    private String buildWaLink(String toNumber, String text) {
        String encoded = java.net.URLEncoder.encode(text, StandardCharsets.UTF_8);
        return "https://wa.me/" + toNumber + "?text=" + encoded;
    }

    private String buildMessage(LocalDate date, List<GroceryItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Grocery List - ").append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("*\n\n");

        int i = 1;
        for (GroceryItem item : items) {
            sb.append(i++).append(". ").append(item.getName());
            if (item.getQty() != null && !item.getQty().isBlank()) {
                sb.append(" - ").append(item.getQty());
            }
            if (item.isChecked()) {
                sb.append(" \u2713");
            }
            sb.append("\n");
        }
        sb.append("\nSent from Grocery List app");
        return sb.toString();
    }
}
