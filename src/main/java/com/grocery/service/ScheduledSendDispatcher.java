package com.grocery.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfigurationSource;
import com.grocery.model.GroceryItem;
import com.grocery.model.ScheduledSend;
import com.grocery.repository.ScheduledSendRepository;

// Split out of ScheduledSendService so the send itself can be @Async.
@Service
public class ScheduledSendDispatcher {

    private final CustomUserDetailsService customUserDetailsService;

    private final CorsConfigurationSource corsConfigurationSource;

	private final ScheduledSendRepository scheduledSendRepository;
	private final WhatsAppService whatsAppService;
	private final EmailService emailService;

	public ScheduledSendDispatcher(ScheduledSendRepository scheduledSendRepository, WhatsAppService whatsAppService,
			EmailService emailService, CorsConfigurationSource corsConfigurationSource, CustomUserDetailsService customUserDetailsService) {
		this.scheduledSendRepository = scheduledSendRepository;
		this.whatsAppService = whatsAppService;
		this.emailService = emailService;
		this.corsConfigurationSource = corsConfigurationSource;
		this.customUserDetailsService = customUserDetailsService;
	}

	@Async("scheduledSendExecutor")
	public void dispatch(ScheduledSend schedule, LocalDate today, List<GroceryItem> items) {
		if (!items.isEmpty()) {
			if ("EMAIL".equals(schedule.getChannel().toString().toUpperCase())) {
				emailService.sendGroceryList(schedule.getUsername(), schedule.getToNumber(), today, items, true);
			} else {
				whatsAppService.sendGroceryList(schedule.getUsername(), schedule.getToNumber(), today, items, true);
			}
		}
		schedule.setEnabled(false);
		scheduledSendRepository.save(schedule);
	}
}
