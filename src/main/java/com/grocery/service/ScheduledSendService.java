package com.grocery.service;

import com.grocery.dto.ScheduledSendRequest;
import com.grocery.model.GroceryItem;
import com.grocery.model.ScheduledSend;
import com.grocery.repository.ScheduledSendRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Handles "send my list automatically every <day> at <time>". This only
// works while the server process is actually running at that minute - see
// the note on Render's free tier in the controller/README.
@Service
public class ScheduledSendService {

	private final ScheduledSendRepository scheduledSendRepository;
	private final GroceryService groceryService;
	private final WhatsAppService whatsAppService;
	private final EmailService emailService;

	public ScheduledSendService(ScheduledSendRepository scheduledSendRepository, GroceryService groceryService,
			WhatsAppService whatsAppService, EmailService emailService) {
		this.scheduledSendRepository = scheduledSendRepository;
		this.groceryService = groceryService;
		this.whatsAppService = whatsAppService;
		this.emailService = emailService;
	}

	public Optional<ScheduledSend> get(String username) {
		return scheduledSendRepository.findByUsername(username);
	}

	public ScheduledSend save(String username, ScheduledSendRequest request) {
		ScheduledSend schedule = scheduledSendRepository.findByUsername(username).orElseGet(() -> {
			ScheduledSend s = new ScheduledSend();
			s.setUsername(username);
			return s;
		});

		String channel = request.getChannel() == null || request.getChannel().isBlank() ? "WHATSAPP"
				: request.getChannel().toUpperCase();

		schedule.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
		schedule.setDayOfWeek(DayOfWeek.valueOf(request.getDayOfWeek().toUpperCase()));
		schedule.setHour(request.getHour());
		schedule.setMinute(request.getMinute());
		schedule.setChannel(channel);
		// Only strip to digits for a phone number - an email address needs its
		// punctuation kept.
		schedule.setToNumber("EMAIL".equals(channel) ? request.getToNumber().trim()
				: request.getToNumber().replaceAll("[^0-9]", ""));
		return scheduledSendRepository.save(schedule);
	}

	// Runs once a minute. Cheap: only does real work on the exact minute a
	// schedule is due, and lastTriggeredDate stops it firing twice.
	@Scheduled(cron = "0 * * * * *")
	public void checkAndSend() {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = now.toLocalDate();
		List<ScheduledSend> due = scheduledSendRepository.findByEnabledTrue();

		for (ScheduledSend schedule : due) {
			boolean isDue = schedule.getDayOfWeek() == now.getDayOfWeek() && schedule.getHour() == now.getHour()
					&& schedule.getMinute() == now.getMinute();
			boolean alreadySentToday = today.equals(schedule.getLastTriggeredDate());

			if (isDue && !alreadySentToday) {
				List<GroceryItem> items = groceryService.getItems(schedule.getUsername(), today);
				if (!items.isEmpty()) {
					if ("EMAIL".equals(schedule.getChannel())) {
						emailService.sendGroceryList(schedule.getUsername(), schedule.getToNumber(), today, items,
								true);
					} else {
						whatsAppService.sendGroceryList(schedule.getUsername(), schedule.getToNumber(), today, items,
								true);
					}
				}
				schedule.setEnabled(false);
				schedule.setLastTriggeredDate(today);
				scheduledSendRepository.save(schedule);
			}
		}
	}
}
