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
@Service
public class ScheduledSendService {

	private final ScheduledSendRepository scheduledSendRepository;
	private final GroceryService groceryService;
	private final ScheduledSendDispatcher dispatcher;

	public ScheduledSendService(ScheduledSendRepository scheduledSendRepository, GroceryService groceryService,
			ScheduledSendDispatcher dispatcher) {
		this.scheduledSendRepository = scheduledSendRepository;
		this.groceryService = groceryService;
		this.dispatcher = dispatcher;
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
		schedule.setToNumber("EMAIL".equals(channel) ? request.getToNumber().trim()
				: request.getToNumber().replaceAll("[^0-9]", ""));
		schedule.setGroceryDateTimeStamp(request.getGroceryTimeStamp());
		return scheduledSendRepository.save(schedule);
	}

	// Runs once a minute. Cheap: only does real work on the exact minute a
	// schedule is due, and lastTriggeredDate stops it firing twice.
	@Scheduled(cron = "0 * * * * *")
	public void checkAndSend() {
		LocalDateTime now = LocalDateTime.now();
		List<ScheduledSend> due = scheduledSendRepository.findByEnabledTrue();

		for (ScheduledSend schedule : due) {
			boolean isDue = schedule.getDayOfWeek().toString().toLowerCase()
					.equals(now.getDayOfWeek().toString().toLowerCase()) && schedule.getHour() == now.getHour()
					&& schedule.getMinute() == now.getMinute();

			if (isDue) {
				List<GroceryItem> items = groceryService.getItems(schedule.getUsername(),
						schedule.getGroceryDateTimeStamp().toLocalDate());
				// Hands the actual send + save off to the async executor and returns
				// immediately, so one slow WhatsApp/SMTP call can't stall this loop
				dispatcher.dispatch(schedule, schedule.getGroceryDateTimeStamp().toLocalDate(), items);
			}
		}
	}
}
