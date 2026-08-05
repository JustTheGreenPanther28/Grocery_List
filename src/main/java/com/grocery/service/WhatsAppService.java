package com.grocery.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.grocery.dto.WhatsAppSendResponse;
import com.grocery.model.GroceryItem;
import com.grocery.model.SentMessage;
import com.grocery.repository.SentMessageRepository;

@Service
public class WhatsAppService {

	@Value("${whatsapp.phone-number-id:}")
	private String phoneNumberId;

	@Value("${whatsapp.access-token:}")
	private String accessToken;

	// stalls; these bound worst-case time to fail so a stuck request can't
	// tie up a thread forever.
	private final RestTemplate restTemplate = new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5))
			.setReadTimeout(Duration.ofSeconds(10)).build();
	private final SentMessageRepository sentMessageRepository;

	public WhatsAppService(SentMessageRepository sentMessageRepository) {
		this.sentMessageRepository = sentMessageRepository;
	}

	//pre-approved template
	// outside the 24-hour messaging window. Use the "grocery" template instead.
	public WhatsAppSendResponse sendGroceryList(String username, String toNumber, LocalDate date,
			List<GroceryItem> items, boolean automatic) {
		String digitsOnlyNumber = toNumber.replaceAll("[^0-9]", "");

		boolean apiConfigured = phoneNumberId != null && !phoneNumberId.isBlank() && accessToken != null
				&& !accessToken.isBlank();

		WhatsAppSendResponse response;
		if (apiConfigured) {
			try {
				sendViaCloudApi(digitsOnlyNumber, date, items);
				response = new WhatsAppSendResponse(true, "Message sent via WhatsApp Cloud API.", null);
			} catch (Exception ex) {
				// Fall back to a wa.me link if the direct send fails
				String text = buildPlainTextFallback(date, items);
				String link = buildWaLink(digitsOnlyNumber, text);
				System.out.println("Exception class: " + ex.getClass().getName());
				System.out.println("Exception message: " + ex.getMessage());
				response = new WhatsAppSendResponse(false,
						"Direct send failed (" + ex.getMessage() + "). Use the link instead.", link);
			}
		} else {
			String text = buildPlainTextFallback(date, items);
			String link = buildWaLink(digitsOnlyNumber, text);
			response = new WhatsAppSendResponse(false,
					"WhatsApp Cloud API not configured on the server - open this link to send manually.", link);
		}

		// Log: we tried to send via the "grocery" template (even if we fell back to
		// wa.me)
		String logText = "[Template: grocery] Sent to " + digitsOnlyNumber + " for " + date;
		sentMessageRepository.save(new SentMessage(username, date, digitsOnlyNumber, "WHATSAPP", logText,
				response.isSentDirectly(), automatic));
		return response;
	}

	private void sendViaCloudApi(String toNumber, LocalDate date, List<GroceryItem> items) {

		String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

		String itemsText = buildItemsText(items);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("messaging_product", "whatsapp");
		body.put("to", toNumber);
		body.put("type", "template");

		Map<String, Object> template = new LinkedHashMap<>();
		template.put("name", "grocery");
		template.put("language", Map.of("code", "en"));

		List<Map<String, Object>> parameters = List.of(
				Map.of("type", "text", "parameter_name", "date", "text", date.toString()),
				Map.of("type", "text", "parameter_name", "items", "text", itemsText));

		template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));

		body.put("template", template);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(accessToken);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

	}

	private String buildWaLink(String toNumber, String text) {
		String encoded = java.net.URLEncoder.encode(text, StandardCharsets.UTF_8);
		return "https://wa.me/" + toNumber + "?text=" + encoded;
	}

	// Format items for the {{items}} placeholder in the template
	// Format items for the {{items}} placeholder in the template
	private String buildItemsText(List<GroceryItem> items) {
		StringBuilder sb = new StringBuilder();

		int i = 1;
		for (GroceryItem item : items) {

			if (sb.length() > 0) {
				sb.append(", ");
			}

			sb.append(i++).append(". ").append(item.getName());

			if (item.getQty() != null && !item.getQty().isBlank()) {
				sb.append(" - ").append(item.getQty());
			}

			if (item.isChecked()) {
				sb.append(" ✓");
			}
		}

		return sb.toString().replace("\n", " ").replace("\r", " ").replace("\t", " ").replaceAll(" {2,}", " ").trim();
	}

	// Fallback plain text for wa.me link (when template can't be used)
	private String buildPlainTextFallback(LocalDate date, List<GroceryItem> items) {
		StringBuilder sb = new StringBuilder();
		sb.append("*Grocery List - ").append(date).append("*\n\n");

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